Cryptfs Password Manager
========================

Android device encryption password manager app. Let's you changes the Android
disk encryption password. Essentially the same as 

   vdc cryptfs changepw <newpassword>

but easier to use and slightly more foolproof. Requires root access.

**WARNING**

If you forget the new password after you change it, you will not be able to boot  the device. You will have to perform a factory reset, DELETING all your data. Make sure you take a full backup before using this tool, and REMEMBER THE PASSWORD. You have been warned, use at your own risk!
 
Android 3.0 (Honeycomb) introduced disk encryption and it has been available on all subsequent versions. It encrypts the data partition with a key protected by a user-selected password and requires entering the password in order to boot the device. However, Android uses the device unlock password or PIN as the device encryption password, and doesn't allow you to change them separately. This effectively forces you  to use a simple password, since you have to enter it each time you unlock your device, usually dozens of times a day. This tool allows you to change the encryption password to a more secure one, without affecting the screen unlock password/PIN. To change the device encryption password simply: 

 1. Enter the current password
 (initially the same as the unlock password/PIN)
 2. Enter and confirm the new password
 3. Hit 'Change password'

The changes take effect immediately, but you will only be required to enter the new password the next time you boot your device. Make sure you choose a good password, not based on a dictionary word, since automated tools can brute force a simple password in minutes. Above all, make sure you REMEMBER the new password. 

If you change the device unlock password/PIN, the encryption password will be automatically changed as well. You need to use this tool again to change it back, if required. 

The app is also available in the Google Play Store: 

https://play.google.com/store/apps/details?id=org.nick.cryptfs.passwdmanager

Borrows some code from https://github.com/project-voodoo/ota-rootkeeper-app, 
under the WTFPL license. 

