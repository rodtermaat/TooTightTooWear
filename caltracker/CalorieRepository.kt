import android.content.Context
import android.content.SharedPreferences
import java.util.prefs.Preferences

const val PREFERENCE_NAME = "TOOTIGHT_APP_PREF"

class CalorieRepository(val context: Context) {

    private val pref: SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    //private val pref: Preferences = context.getPreferences(Context.MODE_PRIVATE)
    //val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

    private val editor = pref.edit()

    //private val gson = Gson()

    private fun String.put(int: Int) {
        editor.putInt(this, int)
        editor.commit()
    }

    private fun String.getInt() = pref.getInt(this, 0)


}
