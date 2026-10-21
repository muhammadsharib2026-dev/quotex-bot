package com.example.quotexfloat
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
object Market {
 data class Result(val price:Double,val signal:String,val reason:String)
 fun analyze():Result {
 val conn=URL("https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=100").openConnection() as HttpURLConnection
 conn.connectTimeout=8000;conn.readTimeout=8000
 try {
 if(conn.responseCode!=200)throw Exception("HTTP ${conn.responseCode}")
 val arr=JSONArray(conn.inputStream.bufferedReader().use{it.readText()})
 val closes=(0 until arr.length()-1).map{arr.getJSONArray(it).getDouble(4)}
 if(closes.size<30)throw Exception("Not enough candles")
 fun ema(n:Int):Double{val k=2.0/(n+1);var v=closes.take(n).average();for(i in n until closes.size)v=closes[i]*k+v*(1-k);return v}
 var gains=0.0;var losses=0.0
 for(i in closes.size-14 until closes.size){val d=closes[i]-closes[i-1];gains+=max(0.0,d);losses+=max(0.0,-d)}
 val rsi=if(gains+losses==0.0)50.0 else 100*gains/(gains+losses)
 val e9=ema(9);val e21=ema(21)
 val signal=when{e9>e21&&rsi in 52.0..68.0->"UP";e9<e21&&rsi in 32.0..48.0->"DOWN";else->"WAIT"}
 return Result(closes.last(),signal,"EMA9 %.1f EMA21 %.1f RSI %.1f (exchange, not Quotex)".format(e9,e21,rsi))
 }finally{conn.disconnect()}
 }
}