package com.savagellc.raven.include

import org.json.JSONObject

class Me(val obj: JSONObject) {
    val id = obj.getString("id")
    val username = obj.getString("username")
}
