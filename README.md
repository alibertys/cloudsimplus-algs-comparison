# cloudsimplus-algs-comparison
Simulation of various allocation and scheduling algorithms in a specific cloud system
***
This project compares different scenarios that uses distinct combinations of allocation and scheduling policies using CloudSimPlus library.
The allocation policies are: Simple (Worst-Fit) | Best-Fit | First-Fit
The scheduling policies are: Time-shared | Space-shared
***
You can describe your own systems in classes HomogeneousSystemComparison and HeterogeneousSystemComparison
SystemComparisonBase class used to hold all the neccessary operations to simulate the cloud system
Configurations are used to read and update config.json, as well as do some utility for short codes
***
When running the program input all the needed info using shortcodes like S | FF | BF | TS | SS | OFF...
The results are written into csv file
