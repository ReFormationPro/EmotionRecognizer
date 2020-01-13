# EmotionRecognizer
An application to record your daily feelings while you use your mobile phone. Made on a hackathon.

# Used APIs
<strong>TensorFlow Support Common:</strong> Detection of facial expressions.

<strong>Google FaceDetect API:</strong> To use TensorFlow for detection of facial expression, we needed to crop the face and feed it into TensorFlow. Google FaceDetect API solved this issue nicely.

<strong>SQLite Database:</strong> We created a DatabaseHelper extending SQLiteOpenHelper for recording emotions. This feature is not completed in this demo though.

<strong>Android Camera2 API:</strong> Since we needed to take photographs from front camera at anytime screen is on (sounds creepy but everything is local), we could not rely on other apps. So we created our own.

<strong>PhilJay's MPAndroidChart:</strong> Charts to show our statistics. Needed for later.

# Demo Images
<img src="https://raw.githubusercontent.com/ReFormationPro/EmotionRecognizer/master/1.png" width="400"></src>
<img src="https://raw.githubusercontent.com/ReFormationPro/EmotionRecognizer/master/2.png" width="400"></src>

# Usage Tips
Read comments above classes. There are alternative scripts, by looking at those comments you can know which one is better to use (they are only commented out to make the demo work).

Function named "Newest" is the latest function assigned for "Analyze Latest Capture" button. It is a good example of how to recognize facial expression from a bitmap:

<strong>1-</strong> Use Google Face Detector to crop the face on the image.

<strong>2-</strong> Resize to 48x48 and transform to grayscale the cropped image to get a resized image ready to be classified.

<strong>3-</strong> Use Image Classifier's classifyFrame method to get a HashMap<String, Float> of results of emotions and their probabilities.

<strong>4-</strong> Optional. Using DatabaseHelper, save emotions, application name and the timestamp to the database. Planned but demo does not demonstrate this.

<strong>5-</strong> Optional. Take and save the screenshot of your phone at the time of recording. Planned but not provided in the demo.

<strong>6-</strong> Optional. Generate statistics from your database and combine them with MPAndroidChart. Planned but not provided in the demo.

# Some Notes
We wanted to release a well-working demo so we needed to revert to latest working release. Therefore, some of the classes in this repo are completely commented out. As mentioned in the comments over them, they are the stable version of code. 
