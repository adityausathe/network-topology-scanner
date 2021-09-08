# Network topology scanner

### :heavy_check_mark: Working
### :warning: Historical Project
This utility is developed as an undergraduate mini-project. Its source-code does not adhere to standard conventions/practices and needs refactoring. I've tried to tag the areas where refactoring is needed, consider going through those before using this.

## Functionality
- The tool asks user to enter start and end IP address, and scans the range to find out the topology with respect to host machine.
- It then draws a tree-based representation of the topology with host machine at root. Other network devices are shown at levels based on the number of hops between them and the host.

## Implementation 
- The tool uses **MyTraceRoute(mtr)** and **nmap** utilities to perform network scan.
- It uses a binary-search based scan-heuristic to minimize unnecessary scans.
- Basically, it determines which IP addresses to scan from the given range, so that the scan-count and latency will be minimized.

## Dependencies
- Current version works on Linux only
- Needs pre-installed mtr and nmap utilities 
