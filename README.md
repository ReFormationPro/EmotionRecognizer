# EmotionRecognizer
An application to record your daily feelings while you use your mobile phone. Made on a hackathon.

# Used APIs
TensorFlow Support Common: Detection of facial expressions.
Google FaceDetect API: To use TensorFlow for detection of facial expression, we needed to crop the face and feed it into TensorFlow. Google FaceDetect API solved this issue nicely.
SQLite: We created a DatabaseHelper extending SQLiteOpenHelper for recording emotions. This feature is not completed in this demo though.
Android Camera2 API: Since we needed to take photographs from front camera at anytime screen is on (sounds creepy but everything is local), we could not rely on other apps. So we created our own.
PhilJay's MPAndroidChart: Charts to show our statistics. Needed for later.

# Demo Images
![alt text](https://raw.githubusercontent.com/ReFormationPro/EmotionRecognizer/master/1.png)
![alt text](https://raw.githubusercontent.com/ReFormationPro/EmotionRecognizer/master/2.png)

# Usage Tips
Read comments above classes. There are alternative scripts, by looking at those comments you can know which one is better to use (they are only commented out to make the demo work).
"Newest" named function is the latest function assigned for "Analyze Latest Capture" button. There you will see Google's FaceDetector is used to crop the image (we must have only the face) and then resulting image is feeded into image processor. Then Image classifier is used to call "classifyFrame" on the cropped image to get a HashMap result of emotions and their probabilities. You can make this routine anywhere and then use DatabaseHelper to save your emotion data.
Once you are done with creating the database, you can try to visualize them on charts.

# Some Notes
We wanted to release a well-working demo so we needed to revert to latest working release. Therefore, some of the classes in this repo are completely commented out. As mentioned in the comments over them, they are the stable version of code. 
