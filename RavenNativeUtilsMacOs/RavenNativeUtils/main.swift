//
//  main.swift
//  RavenNativeUtils
//
//  Created by Yann Holme Nielsen on 14.08.19.
//  Copyright © 2019 21 Xayah. All rights reserved.
//

import Foundation
import Cocoa
func showNotification(title: String?, message:String?, image:String?) {
        let notification = NSUserNotification()

        notification.title = title ?? ""
        notification.informativeText = message ?? ""
        if image != "" {
        notification.contentImage = NSImage(contentsOf: NSURL(string: image ?? "")! as URL)
        }
        notification.soundName = NSUserNotificationDefaultSoundName
        NSUserNotificationCenter.default.deliver(notification)
    }
    
let title = readLine()
let message = readLine()
let image = readLine()
showNotification(title: title, message: message, image: image)
sleep(2)
