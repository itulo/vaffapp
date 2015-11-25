Intro
=======
This is the codebase of the android app VaffApp, available from the Google Play Store, [here](https://play.google.com/store/apps/details?id=italo.vaffapp.app).
The app gathers insults from different italian dialects, grouped by region, with an interpretation in standard italian. The user can surf insults randomly and share them.

# Technical details
All the insults are stored in an SQlite database which I update from time to time.
The app implements rewarding and in-app purchases. Rewards (more insults) are given when the user opens the app every new day, and when they share any insults for three times. In-app purchases allow the user to obtain all insults in the database, and to unlock a new view where they can see all the insults in a list.
There are 4 activities:
* MainActivity: shows a welcoming message, buttons to other activities and buttons to do in-app purchases. It also shows an ad when the user wants too. It hides the button to SendInsultActivity when the UI is in english.
* InsultActivity: shows an insult randomly. It allows the user to share.
* SendInsultActivity: allows the user to suggest an insult or send a feedback.
* InsultListActivity: InsultDetailActivity, InsultDetailFragment and InsultListFragment are part of this activity too. Here the user can look at all insults in a list, and select one for more details. This activity is hidden in the beginning and it can be unlocked by buying it.

# Getting Started
Download Android Studio and open the current directory as an existing project.
In addition you need to add to the app/libs folder:
* Vungle SDK v3.2.2 (I had a compatibility problem with support-v4-18.0.0.jar so I skipped it)
* Flurry SDK v6.0.0

Also you need to add the Facebook SDK (links that were useful for me: [link1](http://stackoverflow.com/questions/22382905/import-facebook-sdk-on-android-studio-0-5-1) [link2](http://stackoverflow.com/questions/21477884/couldnt-import-library-project-android-studio))

Finally, change the path to the android sdk in local.properties.

# Disclaimer
In the beginning I was not planning to share this codebase on github. The code is organized in a sensible way IMHO, however I know there is still room for refactoring. Also, there is some repetition somewhere.
There are no tests, pardon me.

# Usage of this code
If you see a solution to your problem feel free to copy the code (please mention where you got it from). You are free to offer me a coffee by: watching an ad, making an in-app purchase :)

# Contact
For any communication feel free to contact me at italo.armenti at gmail.com
