# Tomo app


### How to use

## Installing Proximi io sdk

Main folder is "pr-support"

Clone or download this repo and open it with Android Studio as an existing project.
After that you need to replace the auth key placeholder with your own auth key (in MainActivity.java), which can be found in your portal under "Applications":

```
public static final String AUTH = "AUTH_KEY_HERE"; // TODO: Replace with your own!
```
## Installing Steerpath sdk

Main folder is "sp-support"

Go to ExampleApplication class

Place your api-key here
SteerpathClient.StartConfig config =  new SteerpathClient.StartConfig.Builder()
                // MANDATORY:
                .name("")
                .apiKey("OUR API_KEY HERE")