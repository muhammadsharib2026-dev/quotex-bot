package com.example.quotexfloat
import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

/** Experimental chart-pixel approximations, NOT the Python backend's 9 original detectors.
 * No OHLC feed, volume or tick data is available from a screenshot. Never treat scores as win rates.
 */
object OtcDetector {
 data class Candle(val x:Int,val top:Int,val bottom:Int,val bullish:Boolean)
 data class Result(val signal:String,val details:String,val candles:Int)
 private data class Vote(val name:String,val bias:Int)
 fun analyze(src:Bitmap):Result {
  val w=src.width;val h=src.height
  val left=(w*.10).toInt();val right=(w*.57).toInt()
  val top=(h*.16).toInt();val bottom=(h*.66).toInt()
  if(right<=left||bottom<=top)return Result("WAIT","Invalid chart region",0)
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
  for(c in found)if(groups.isEmpty()||c.x-groups.last().last().x>5)groups.add(mutableListOf(c))else groups.last().add(c)
  val candles=groups.filter{it.size>=2}.map{g->Candle(g.map{it.x}.average().toInt(),g.minOf{it.top},g.maxOf{it.bottom},g.count{it.bullish}>g.size/2)}
  if(candles.size<12)return Result("WAIT","Need 12 visible candle bodies; found ${candles.size}",candles.size)
  val cs=candles.takeLast(12)
  val mid=cs.map{(it.top+it.bottom)/2.0}
  val sizes=cs.map{(it.bottom-it.top).toDouble()}
  val last=cs.last()
  val prev=cs.dropLast(1)
  val up=prev.count{it.bullish}
  val down=prev.size-up
  val avg=sizes.dropLast(1).average()
  val height=(cs.maxOf{it.bottom}-cs.minOf{it.top}).coerceAtLeast(1)
  val delta=(mid[3]-mid[10])/height
  val trend=when{delta>0.12->1;delta < -0.12->-1;else->0}
  val lastBias=if(last.bullish)1 else -1
  val majority=when{up>=8->1;down>=8->-1;else->0}
  val recentBias=cs.takeLast(3).sumOf{if(it.bullish)1 else -1}.let{if(abs(it)==3)if(it>0)1 else -1 else 0}
  val strong= sizes.last()>=avg*0.7 && sizes.last()<=avg*2.5
  val rangeTop=cs.dropLast(1).minOf{it.top}
  val rangeBottom=cs.dropLast(1).maxOf{it.bottom}
  val nearHigh=last.top<=rangeTop+height*0.12
  val nearLow=last.bottom>=rangeBottom-height*0.12
  val votes=listOf(
   Vote("Market structure",trend),
   Vote("Support/resistance",if(nearLow&&last.bullish)1 else if(nearHigh&&!last.bullish)-1 else 0),
   Vote("Price action",if(strong)lastBias else 0),
   Vote("Liquidity",0), // tick-level sweeps unavailable
   Vote("Order blocks",0), // requires reliable OHLC zones
   Vote("FVG",0), // gaps cannot be verified from body pixels
   Vote("Supply/demand",if(nearLow&&last.bullish)1 else if(nearHigh&&!last.bullish)-1 else 0),
   Vote("Volume proxy",0), // no volume data
   Vote("OTC patterns",if(recentBias==lastBias)recentBias else 0)
  )
  val positive=votes.count{it.bias>0};val negative=votes.count{it.bias<0}
  // Missing-data detectors abstain, never fabricated as supporting evidence.
  val signal=when{
   trend==1&&majority==1&&positive>=3&&negative==0&&strong->"UP"
   trend== -1&&majority== -1&&negative>=3&&positive==0&&strong->"DOWN"
   else->"WAIT"
  }
  val available=votes.count{it.bias!=0}
  return Result(signal,"9 categories checked: ${positive} bullish / ${negative} bearish; ${available} directional. 4 categories lack required feed. Experimental screenshot analysis; unvalidated.",candles.size)
 }
}
