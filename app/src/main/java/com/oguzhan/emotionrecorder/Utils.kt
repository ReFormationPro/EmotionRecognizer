package com.oguzhan.emotionrecorder

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception

class Utils {
    companion object {
        fun getForegroundApplication(context: Context): String {
            val activityManager = context.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
            val packageManager = context.getPackageManager()

            val foregroundTaskInfo = activityManager.getRunningTasks(1).get(0);
            val foregroundTaskPackageName = foregroundTaskInfo.topActivity.getPackageName()
            try {
                packageManager.getPackageInfo(foregroundTaskPackageName, 0);
            } catch (e: Exception) {
                e.printStackTrace();
            }
            var foregroundAppPackageInfo: PackageInfo? = null
            var foregroundTaskAppName = ""
            try {
                foregroundAppPackageInfo = packageManager.getPackageInfo(foregroundTaskPackageName, 0) as PackageInfo
                foregroundTaskAppName = foregroundAppPackageInfo!!.applicationInfo.loadLabel(packageManager).toString();
            } catch (e: Exception) {
                e.printStackTrace();
            }
            return foregroundTaskAppName
        }
        class MapValueComparator(map: HashMap<String, Float>) : Comparator<String> {
            var map: HashMap<String, Float> = HashMap<String, Float>()
            init {
                this.map.putAll(map)
            }
            override fun compare(s1: String, s2: String): Int {
                return if (map[s1]!! >= map[s2]!!) {
                    -1
                } else {
                    1
                }
            }
        }
    }
}