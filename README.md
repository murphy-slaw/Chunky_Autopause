# Chunky Autopause
Chunky Autopause is a utility mod for the [Chunky](https://modrinth.com/plugin/chunky) chunk pregeneration mod. It monitors the server population, pausing running Chunky tasks when there are players online and resuming them when the server is empty.

Obviously, this requires Chunky to be installed.

## Commands
```
/cap enable
/cap disable
```

## Configuration
```
#Whether to enable automatically pausing Chunky on startup
enableOnStartup = true
#How many ticks to wait until resuming Chunky tasks after the last player logs out
#Range: > 0
resumeWaitTicks = 600
```
## Acknowledgements
Inspired by [Chunky Extension](https://modrinth.com/mod/chunky-extension) for Fabric.
