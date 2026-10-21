package com.example.quotexfloat
import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*
object OtcDetector {
 data class Candle(val x:Int,val top:Int,val bottom:Int,val bullish:Boolean)
 data class Result(val signal:String,val details:String,val candles:Int)
 // Detect visible candle bodies from green/red pixel runs, not proprietary OTC prices.
 fun analyze(src:Bitmap):Result {
  val w=src.width;val h=src.height
  val left=(w*.10).toInt();val right=(w*.57).toInt()
  val top=(h*.16).toInt();val bottom=(h*.66).toInt()
  if(right<=left||bottom<=top)return Result("WAIT","Invalid capture",0)
  val found=mutableListOf<Candle>()
  for(x in left until right step 2){
   var green=0;var red=0;var hi=h;var lo=0
   for(y in top until bottom step 2){
    val c=src.getPixel(x,y);val r=Color.red(c);val g=Color.green(c);val b=Color.blue(c)
    val up=g>95&&g>r*1.4&&g>b*1.2
    val down=r>110&&r>g*1.35&&r>b*1.3
    if(up||down){if(up)green++ else red++;hi=min(hi,y);lo=max(lo,y)}
   }
   if(max(green,red)>=8&&lo-hi>=14)found.add(Candle(x,hi,lo,green>red))
  }
  val groups=mutableListOf<MutableList<Candle>>()
  for(c in found){if(groups.isEmpty()||c.x-groups.last().last().x>5)groups.add(mutableListOf(c))else groups.last().add(c)}
  val candles=groups.filter{it.size>=2}.map{g->Candle(g.map{it.x}.average().toInt(),g.minOf{it.top},g.maxOf{it.bottom},g.count{it.bullish}>g.size/2)}
  if(candles.size<5)return Result("WAIT","Only ${candles.size} visible candle bodies; need at least 5",candles.size)
  val recent=candles.takeLast(5)
  val up=recent.count{it.bullish};val down=5-up
  val last=recent.last()
  // Pixel-only heuristic, explicitly not a verified price feed or prediction.
  val signal=when {up>=4&&last.bullish->"UP";down>=4&&!last.bullish->"DOWN";else->"WAIT"}
  return Result(signal,"Visual heuristic: last 5 candles UP $up / DOWN $down; not validated",candles.size)
 }
}