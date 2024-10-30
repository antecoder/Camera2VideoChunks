This is a simple sample of recording camera video in bits/chunks of a custom size using the Android Camera2 API's. It also includes sequential playback using the Android Exoplayer API.

### How to Run

- Clone the repository to your preferred directory:
  > git clone https://github.com/antecoder/Camera2VideoChunks.git
- Open the project using Android Studio and let gradle sync & download dependencies.
- Run on your camera-enabled device or on the emulator

#### Misc
- You can set the size of the chunk output in the **Constants**.kt file (*app.learning.mediachunkupload.util.Constants*)
- You can find the recorded chunks in this folder on your device (any decent file-manager should take you there) : */Android/data/app.learning.mediachunkupload/files/Videos*.
- Each recorded session is put in a folder with the timestamp of recording as the name