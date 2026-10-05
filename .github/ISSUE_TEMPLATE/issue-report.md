---
name: Issue Report
about: Report a problem with the Janus wallet, with the details needed to reproduce it.
title: ''
labels: bug
assignees: ''

---

<!--
Thank you for reporting an issue! Never paste your wallet.dat, recovery phrase, passwords or private keys here.
-->

### Operating System

- [ ] Windows
- [ ] Linux
- [ ] macOS

### Operating System Version

The version of your operating system (e.g. Windows 11, Ubuntu 24.04, macOS 15).

On Linux, also paste the output of:

```
echo $XDG_SESSION_TYPE $XDG_CURRENT_DESKTOP
```

### Janus Version

1. Janus version (e.g. 2.0.0, from the installer's file name or `dpkg -s unigrid` / `rpm -q unigrid`):
2. Installed from (.deb, .rpm, .tar.gz, .msi or .dmg):

### Description

A clear and concise description of the problem.

### Steps to Reproduce

1. Step 1
2. Step 2
3. Step 3

### Expected Behavior

What you expected to happen.

### Actual Behavior

What actually happened.

### Screenshots

If applicable, add screenshots. You can drag and drop the images here.

### Logs

On Linux and macOS, start Janus from a terminal and paste what it prints:

- Linux: `/opt/unigrid/bin/Unigrid`, or `Unigrid/bin/Unigrid` in the folder unpacked from the .tar.gz
- macOS: `/Applications/Unigrid.app/Contents/MacOS/Unigrid`

If the problem involves syncing or balances, also attach `~/.janus/hedgehog.log`.
