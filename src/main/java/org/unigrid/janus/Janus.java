/*
    The Janus Wallet
    Copyright © 2021 The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.unigrid.janus.model.service.Daemon;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.view.MainWindow;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.apache.commons.lang3.ThreadUtils;
import org.unigrid.janus.model.rpc.entity.Info;

@ApplicationScoped
public class Janus extends BaseApplication {

	@Inject
	private Daemon daemon;

	@Inject
	private RPCService rpc;

	@Inject
	private DebugService debug;

	@Inject
	private WindowService window;

	@Inject
	private MainWindow mainWindow;
	
	@Inject
	private JanusPreloader preloader;

	@PostConstruct
	@SneakyThrows
	private void init() {
	    startDaemon();
	}
	
	public void startDaemon(){
	    //TODO: should this change to spalshScreenInsted
		try {
			daemon.start();
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR,
				e.getMessage(),
				ButtonType.OK);
			a.showAndWait();
		}
		debug.log("Daemon start done.");
	}
	
	@PreDestroy
	@SneakyThrows
	private void destroy() {
	    //TODO: should this change to spalshScreenInsted
	    daemon.stop();
	}

	@Override
	public void start(Stage stage, Application.Parameters parameters) throws Exception {

	    startSplashScreen();
	    
	    startMainWindow();
	}
	
	private void startMainWindow(){
	    		try {
			mainWindow.show();

			mainWindow.bindDebugListViewWidth(0.98);
			debug.setListView((ListView) window.lookup("lstDebug"));

			/*final Info info = rpc.call(new Info.Request(), Info.class);
			Jsonb jsonb = JsonbBuilder.create();
			String result = String.format("Info result: %s", jsonb.toJson(info.getResult()));
			debug.log(result);
      */
			
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR,
				e.getMessage(),
				ButtonType.OK);
			a.showAndWait();
		}
	}
	@SneakyThrows
	private void startSplashScreen() {

	    preloader.show();

	    // poll info call every 30 seconds
	    rpc.pollForInfo(30 * 1000);

	    new Thread(() -> {

		int blockCount = -1;
		
		double progress = 0;
		
		preloader.startSpinner();

		try {
			Thread.sleep(3000);
		    } catch (InterruptedException ex) {
			//TODO: Fix eception handling
		    }
		
		while (blockCount == -1) {
		    
		    Info.Result result = rpc.call(Info.METHOD, Info.Result.class);

		    blockCount = result.getBlocks();
		    		    
		    try {
			Thread.sleep(3000);
		    } catch (InterruptedException ex) {
			//TODO: Fix eception handling
		    }
		    
	        }
		    this.notify();
		    preloader.stopSpinner();
	    }).start();

	    synchronized (this) {
		this.wait();
	    }   
	}
	
	public void restartDaemon(){
	    destroy();
	    startDaemon();
	}
	
}
