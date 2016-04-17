Cryptfs Password Manager
========================

Android device encryption password manager app. Lets you changes the Android
disk encryption password. Essentially the same as 
```
 # vdc cryptfs changepw <newpassword>
```

but easier to use and slightly more foolproof. Requires root access.

For Lollipop (5.x): 

```
# vdc cryptfs changepw password <passphrase in hex>
```

For Marshmallow (6.0): 

```
# vdc cryptfs changepw password <passphrase>
```

You may also need to relax the SELinux policy in order to see the output of 
this command. If you have a recent SuperSU installed, you can use the 
command below: 

```
# supolicy --live 'allow vdc devpts chr_file {read write getattr ioctl}'
```


**WARNING**

If you forget the new password after you change it, you will not be able to
boot  the device. You will have to perform a factory reset, DELETING all your
data. Make sure you take a full backup before using this tool, and REMEMBER THE
PASSWORD. You have been warned, use at your own risk!
 
Why and how to use this
-----------------------

**Lollipop**: On Android 5.0+ devices with hardware-backed keystore support the 
decryption key is bound to the device, so a simple unlock password is not 
necessarily easily crackable. Details here: 

http://source.android.com/devices/tech/encryption/index.html

Also see: 

http://nelenkov.blogspot.com/2014/10/revisiting-android-disk-encryption.html

**CyanogenMod 11**: CM11 has a built-in system UI for setting the encryption 
password, so you do not need this tool. That said, it should work fine on CM11, 
but it may not work on CM12.

**CyanogenMod 13**: CM13 (and some vendors, most notably LG) seems to have 
changed the syntax of the `cryptfs` utility. Using the wrong syntax might 
render your data useless, exercise caution. 

```
Usage: cryptfs changepw default|password|pin|pattern [currentpasswd] default|password|pin|pattern [newpasswd]
```

Android 3.0 (Honeycomb) introduced disk encryption and it has been available on
all subsequent versions. It encrypts the data partition with a key protected by
a user-selected password and requires entering the password in order to boot 
the device. However, Android uses the device unlock password or PIN as the 
device encryption password, and doesn't allow you to change them independently.
This effectively forces you  to use a simple password, since you have to enter 
it each time you unlock your device, usually dozens of times a day. This tool 
allows you to change the encryption password to a more secure one, without 
affecting the screen unlock password/PIN. To change the device encryption 
password simply: 

 1. Enter the current password
 (initially the same as the unlock password/PIN)
 2. Enter and confirm the new password
 3. Hit 'Change password'

(If you are using a pattern lock (5.0+), enter the dots as a sequence of 
numbers, where '1' is top left and '9' -- bottom right.)

The changes take effect immediately, but you will only be required to enter 
the new password the next time you boot your device. Make sure you choose a 
good password, not based on a dictionary word, since automated tools can brute 
force a simple password in minutes. Above all, make sure you REMEMBER the new 
password. 

If you change the device unlock password/PIN, the encryption password will be 
automatically changed as well. You need to use this tool again to change it 
back, if required. 

Once Android adds an official way (system UI) to change the passwords 
independently, this tool will no longer be needed. Star this issue if you 
want this to happen:

http://code.google.com/p/android/issues/detail?id=29468


How to get it
-------------

The app is also available in the Google Play Store: 

https://play.google.com/store/apps/details?id=org.nick.cryptfs.passwdmanager

Acknowledgments
---------------

Borrows some code from https://github.com/project-voodoo/ota-rootkeeper-app, 
under the WTFPL license. 

