package dev.rakib.bushra

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LocalStore(context: Context) {
    private val prefs=context.getSharedPreferences("bushra_private_v1",Context.MODE_PRIVATE)
    private fun key():SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (ks.getKey("bushra_secrets_v1",null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("bushra_secrets_v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generateKey()
        }
    }
    fun saveSecret(id:String,value:String) {
        if(value.isBlank()){prefs.edit().remove("secret_$id").apply();return}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val data=cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("secret_$id",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(data,Base64.NO_WRAP)).apply()
    }
    fun secret(id:String):String {
        val raw=prefs.getString("secret_$id",null) ?: return ""
        return try {
            val parts=raw.split(':');val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8)
        } catch (_:Exception) { "" }
    }
    fun text(key:String,default:String="")=prefs.getString(key,default) ?: default
    fun put(key:String,value:String) { prefs.edit().putString(key,value).apply() }
    fun flag(key:String,default:Boolean=false)=prefs.getBoolean(key,default)
    fun putFlag(key:String,value:Boolean) { prefs.edit().putBoolean(key,value).apply() }
    private fun array(key:String):JSONArray=runCatching { JSONArray(text(key,"[]")) }.getOrDefault(JSONArray())
    fun providers()=(0 until array("providers").length()).mapNotNull { runCatching { Provider.parse(array("providers").getJSONObject(it)) }.getOrNull() }
    fun saveProviders(items:List<Provider>)=put("providers",JSONArray(items.map { it.json() }).toString())
    fun contacts()=(0 until array("aliases").length()).mapNotNull { runCatching { ContactAlias.parse(array("aliases").getJSONObject(it)) }.getOrNull() }
    fun saveContacts(items:List<ContactAlias>)=put("aliases",JSONArray(items.map { it.json() }).toString())
    fun history()=(0 until array("history").length()).mapNotNull { runCatching { val j=array("history").getJSONObject(it);ChatLine(j.getString("who"),j.getString("text"),j.getLong("time")) }.getOrNull() }
    fun saveHistory(items:List<ChatLine>)=put("history",JSONArray(items.takeLast(200).map { it.json() }).toString())
    fun tasks()=(0 until array("tasks").length()).mapNotNull { runCatching { val j=array("tasks").getJSONObject(it);TaskEntry(j.getString("text"),j.getString("status"),j.getLong("time")) }.getOrNull() }
    fun saveTasks(items:List<TaskEntry>)=put("tasks",JSONArray(items.takeLast(100).map { it.json() }).toString())
}
