
package org.unigrid.janus.model.filesystem.memoryfs.linux;

import com.github.jnrwinfspteam.jnrwinfsp.WinFspStubFS;
import com.github.jnrwinfspteam.jnrwinfsp.api.*;
import com.github.jnrwinfspteam.jnrwinfsp.service.ServiceException;
import com.github.jnrwinfspteam.jnrwinfsp.service.ServiceRunner;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jnr.ffi.Pointer;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualAbstractPath;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualDirectory;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualFile;
import org.unigrid.janus.model.filesystem.memoryfs.WinFspInfo;
import org.unigrid.janus.model.signal.UsedSpace;

public class WinFspMem extends WinFspStubFS {
	
	private final static String SEPARATOR = "\\";
	
	private static final String ROOT_SECURITY_DESCRIPTOR =
				"O:BAG:BAD:PAR(A;OICI;FA;;;SY)(A;OICI;FA;;;BA)(A;OICI;FA;;;WD)";
	
	private static final long MAX_FILE_NODES = 10240;
	private static final long MAX_FILE_SIZE = 16 * 1024 * 1024;
	
	@Inject
	private Event<UsedSpace> usedSpaceEvent;
	
	private final Supplier<String> rootPathSupplier = () -> {
		return "J:";
	};
	
	private final VirtualDirectory<WinFspInfo> rootDirectory = new VirtualDirectory(SEPARATOR, "\\", rootPathSupplier);
	private final Path rootPath;

	private long nextIndexNumber;
	private String volumeLabel;

	public WinFspMem(Event<UsedSpace> usedSpaceEvent) throws NTStatusException {
		this(false,usedSpaceEvent);
	}

	public WinFspMem(boolean verbose, Event<UsedSpace> usedSpaceEvent) throws NTStatusException {
		this.rootPath = Path.of("\\").normalize();
		this.nextIndexNumber = 1L;
		this.volumeLabel = "Unigrid";
		
		this.usedSpaceEvent = usedSpaceEvent;
		
		final byte[] descriptor = SecurityDescriptorHandler.securityDescriptorToBytes(ROOT_SECURITY_DESCRIPTOR);
		
		final WinFspInfo info = WinFspInfo.builder().securityToken(descriptor)
			.fileAttributes(Set.of(FileAttributes.FILE_ATTRIBUTE_DIRECTORY)).build();
		rootDirectory.setUserData(info);
	}

	public void winVfsRunner() throws NTStatusException, ServiceException {
		Path mountPoint = Paths.get("J:");

		System.out.printf("Mounting %s ...%n", mountPoint == null ? "" : mountPoint);
		ServiceRunner.mountLocalDriveAsService("WinFspMemFS", this, mountPoint, new MountOptions()
			.setDebug(false)
			.setCase(MountOptions.CaseOption.CASE_SENSITIVE)
			.setSectorSize(512)
			.setSectorsPerAllocationUnit(1)
			.setForceBuiltinAdminOwnerAndGroup(true)
		);
	}

	@Override
	public VolumeInfo getVolumeInfo() {
		return generateVolumeInfo();
	}

	@Override
	public VolumeInfo setVolumeLabel(String volumeLabel) {
		this.volumeLabel = volumeLabel;
		return generateVolumeInfo();
	}	

	@Override
	public Optional<SecurityResult> getSecurityByName(String fileName) throws NTStatusException {
		final VirtualAbstractPath<WinFspInfo> path = getMemoryPath(fileName);
		return Optional.of(new SecurityResult(path.getUserData().getSecurityToken(),
			path.getUserData().getFileAttributes()));
	}

	@Override
	public FileInfo create(String fileName, Set<CreateOptions> createOptions, int grantedAccess,
					Set<FileAttributes> fileAttributes, byte[] securityDescriptor, 
					long allocationSize, ReparsePoint reparsePoint) 
							throws NTStatusException {
		
		final Optional<VirtualAbstractPath<WinFspInfo>> file = rootDirectory.find(fileName);

		if (file.isPresent()) {
			throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION
		}

		boolean directory = false;

		for (CreateOptions option : createOptions) {
			if (CreateOptions.FILE_DIRECTORY_FILE.equals(option)) {
				directory = true;
			}
		}
		final Optional<VirtualAbstractPath<WinFspInfo>> parent = getParentPath(fileName);

		if (directory) {
			((VirtualDirectory) parent.get()).mkdir(VirtualAbstractPath
				.getLastComponent(SEPARATOR, fileName));
		} else {
			((VirtualDirectory) parent.get()).mkfile(VirtualAbstractPath
				.getLastComponent(SEPARATOR, fileName));
		}

		final VirtualAbstractPath newPath = rootDirectory.find(fileName).get();
		
		final WinFspInfo winFspInfo = WinFspInfo.builder().securityToken(securityDescriptor)
			.fileAttributes(fileAttributes).build();
		
		newPath.setUserData(winFspInfo);

		FileInfo info = generateFileInfoFull(newPath);
		
		info.getFileAttributes().addAll(fileAttributes);		
		info.setIndexNumber(nextIndexNumber++);
		
		return info;
	}

	private Optional<VirtualAbstractPath<WinFspInfo>> getParentPath(final String path) {
		return rootDirectory.find(path.substring(0, path.lastIndexOf(SEPARATOR)));
	}
	
	
	@Override
	public FileInfo open(String fileName, Set<CreateOptions> createOptions,	int grantedAccess)
							throws NTStatusException {

		final Optional<VirtualAbstractPath<WinFspInfo>> path = getMemoryPathOptional(fileName);
		
		if (path.isEmpty()) {
			throw new NTStatusException(0xC0000034); // STATUS_OBJECT_NAME_NOT_FOUND
		}		
		usedSpaceEvent.fire(UsedSpace.builder().size(path.get().getFolderSize()).build());
		FileInfo fileInfo = generateFileInfoFull(path.get());
		return fileInfo;
	}

	@Override
	public FileInfo overwrite(String fileName, Set<FileAttributes> fileAttributes,
					boolean replaceFileAttributes,long allocationSize) 
							throws NTStatusException {

		final VirtualAbstractPath path = getMemoryPath(Path.of(fileName).toString());
		FileInfo info = new FileInfo(path.getName());
		
		return info;
	}

	@Override
	public void cleanup(OpenContext ctx, Set<CleanupFlags> flags) {

		final VirtualAbstractPath<WinFspInfo> path = getMemoryPath(ctx.getPath());

		if (flags.contains(CleanupFlags.SET_ARCHIVE_BIT) && path instanceof VirtualFile) {
			path.getUserData().getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
		}

		WinSysTime now = WinSysTime.now();

		if (flags.contains(CleanupFlags.SET_LAST_ACCESS_TIME)) {
			path.getTimeInfo().setAccessTime(now.toInstant());
		}

		if (flags.contains(CleanupFlags.SET_LAST_WRITE_TIME)) {
			path.getTimeInfo().setWriteTime(now.toInstant());
		}

		if (flags.contains(CleanupFlags.SET_CHANGE_TIME)) {
			path.getTimeInfo().setCreationTime(now.toInstant());
		}

		if (flags.contains(CleanupFlags.DELETE)) {
			final VirtualAbstractPath abstractPath = getMemoryPath(Path.of(ctx.getPath()).toString());
			abstractPath.getParent().getChildren().remove(abstractPath);
		}
	}

	@Override
	public long read(String fileName, Pointer pBuffer, long offset, int length)
		throws NTStatusException {
		
		final VirtualAbstractPath path = getMemoryPath(Path.of(fileName).toString());

		byte[] destination = new byte[length];

		final ByteBuffer buffer = ByteBuffer.wrap(destination, 0, length);
		int bytes = ((VirtualFile) path).read(buffer, length, offset);
		
		try {
			pBuffer.put(0, buffer.array(), 0, length); //When offset is 0 than it can copy/read large files
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bytes;				
	}

	private FileInfo generateFileInfoFull(VirtualAbstractPath<WinFspInfo> path) {
		return generateFileInfo(path, path.getPath().toString());
	}

	private FileInfo generateFileInfoName(VirtualAbstractPath<WinFspInfo> path) {
		return generateFileInfo(path, path.getName());
	}
	
	private FileInfo generateFileInfo(VirtualAbstractPath<WinFspInfo> path, String name) {
		final FileInfo info = new FileInfo(name);

		if (path instanceof VirtualFile file) {
			info.setAllocationSize(file.getSize());
			info.setFileSize(file.getSize());
		}
		
		if(Objects.nonNull(path.getUserData())) {
			info.getFileAttributes().addAll(path.getUserData().getFileAttributes());
			info.setCreationTime(WinSysTime.fromInstant(path.getTimeInfo().getCreationTime()));
			info.setLastAccessTime(WinSysTime.fromInstant(path.getTimeInfo().getAccessTime()));
			info.setLastWriteTime(WinSysTime.fromInstant(path.getTimeInfo().getWriteTime()));
			info.setChangeTime(WinSysTime.now());
		}
		return info;
	}

	@Override
	public WriteResult write(String fileName, Pointer pBuffer, long offset, int length,
		boolean writeToEndOfFile, boolean constrainedIo) throws NTStatusException {

		final VirtualAbstractPath path = getMemoryPath(Path.of(fileName).toString());	

		final byte[] destination = new byte[length];		
		pBuffer.get(0, destination, 0, length);		
		ByteBuffer buffer = ByteBuffer.wrap(destination, 0, length);		
		final int bytesWritten = ((VirtualFile) path).write(buffer, length, offset);

		FileInfo info = generateFileInfoFull(path);
		
		return new WriteResult(bytesWritten, info);
	}
	
	private Optional<VirtualAbstractPath<WinFspInfo>> getMemoryPathOptional(String path) {		
		return rootDirectory.find(path);
	}
	
	private VirtualAbstractPath getMemoryPath(String path) {
		if(!path.endsWith("\\")){
			path = path.concat("\\");
		}

		VirtualAbstractPath pp = rootDirectory.find(path).get();
		return pp;
	}

	@Override
	public FileInfo flush(String fileName) throws NTStatusException {
		final VirtualAbstractPath path = getMemoryPath(Path.of(fileName).toString());
		return generateFileInfoName(path);
	}

	@Override
	public FileInfo getFileInfo(OpenContext ctx) throws NTStatusException {
		final VirtualAbstractPath path = getMemoryPath(Path.of(ctx.getPath()).toString());
		FileInfo info = generateFileInfoName(path);
		info.getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
		return info;
	}

	@Override
	public FileInfo setBasicInfo(OpenContext ctx, Set<FileAttributes> fileAttributes,
		WinSysTime creationTime, WinSysTime lastAccessTime, WinSysTime lastWriteTime,
		WinSysTime changeTime) throws NTStatusException {

		final VirtualAbstractPath path = getMemoryPath(ctx.getPath());
		path.getTimeInfo().setAccessTime(lastAccessTime.toInstant());
		path.getTimeInfo().setCreationTime(creationTime.toInstant());
		path.getTimeInfo().setWriteTime(lastWriteTime.toInstant());

		return generateFileInfoName(path);
	}

	@Override
	public FileInfo setFileSize(String fileName, long newSize, boolean setAllocationSize)
		throws NTStatusException {
		final VirtualFile file = (VirtualFile) getMemoryPath(Path.of(fileName).toString());

		if (!setAllocationSize) {
			file.setSize(newSize);
		}

		return generateFileInfoName(file);
	}

	@Override
	public void rename(OpenContext ctx, String newFileName, boolean replaceIfExists)
		throws NTStatusException {

		final VirtualAbstractPath path = getMemoryPath(ctx.getPath().toString());

		final VirtualDirectory parentPath = path.getParent();
		if (parentPath.contains(newFileName) && !path.getName().equals(newFileName)) {
			if (!replaceIfExists) {
				throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION
			}
		}
		Path newFileNamePath = Path.of(newFileName).getFileName();
		path.rename(newFileNamePath.toString());
	}

	@Override
	public byte[] getSecurity(OpenContext ctx) throws NTStatusException {
			final VirtualAbstractPath<WinFspInfo> path = getMemoryPath(Path.of(ctx.getPath()).toString());
			return path.getUserData().getSecurityToken();		
	}

	@Override
	public void setSecurity(OpenContext ctx, byte[] securityDescriptor)
		throws NTStatusException {

		final VirtualAbstractPath<WinFspInfo> path = getMemoryPath(Path.of(ctx.getPath()).toString());
		path.getUserData().setSecurityToken(securityDescriptor);
	}

	@Override
	@SneakyThrows
	public void readDirectory(String fileName, String pattern, String marker,
		Predicate<FileInfo> consumer) throws NTStatusException {
		
		final VirtualDirectory<WinFspInfo> directory = (VirtualDirectory) getMemoryPath(fileName);

		// TODO: Add filtering support later

		if (!directory.equals(rootDirectory)) {
			
			if (marker == null) {				
				if (!consumer.test(generateFileInfo((VirtualDirectory)directory.clone(), "."))) {
					return;
				}
			}
			if (marker == null || marker.equals(".")) {
				//com.github.jnrwinfspteam.jnrwinfsp.memfs.DirObj parentDir = getParentObject(filePath);
				if (!consumer.test(generateFileInfo((VirtualDirectory)directory.getParent().clone(), ".."))) {
					return;
				}
				marker = null;
			}
		}
		for (VirtualAbstractPath<WinFspInfo> path : directory.getChildren()) {
			final FileInfo info = generateFileInfoName(path);
			if (!consumer.test(info)) {
				return;
			}
		}
	}
	
	//Right-click properties on files or folders
	@Override
	public FileInfo getDirInfoByName(String parentDirName, String fileName) throws NTStatusException {	
		String newFileName = "\\" + fileName;		
		final VirtualAbstractPath path = getMemoryPath(Path.of(parentDirName+newFileName).toString());
		FileInfo info = generateFileInfoName(path);
		return info;
	}

	private VolumeInfo generateVolumeInfo() {
		
		return new VolumeInfo(
			MAX_FILE_NODES * MAX_FILE_SIZE,
			(MAX_FILE_NODES - rootDirectory.getChildren().size()) * MAX_FILE_SIZE,
			this.volumeLabel
		);
	}
}

