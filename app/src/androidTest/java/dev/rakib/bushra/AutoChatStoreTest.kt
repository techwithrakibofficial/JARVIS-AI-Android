package dev.rakib.bushra

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AutoChatStoreTest{
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private val store=AutoChatStore(context)
 @Before fun clear(){context.getSharedPreferences("bushra_auto_v1",0).edit().clear().commit()}
 @After fun cleanup(){clear()}
 private fun setup():AutoEvent{
  val now=System.currentTimeMillis()
  val r=AutoRule(id="test-rule",channel="sms",label="Test only",phone="+15550000000",instruction="test",created=now-1000,expires=now+60_000,limit=3)
  store.saveRule(r)
  val e=AutoEvent(id="test-event",ruleId=r.id,channel="sms",phone=r.phone,text="test only, never sent",time=now,signature="unique-signature")
  assertTrue(store.addEvent(e));return e
 }
 @Test fun sameIncomingSignatureQueuesOnlyOnce(){val e=setup();assertFalse(store.addEvent(e.copy(id="duplicate")));assertEquals(1,store.events().size)}
 @Test fun parallelReservationsCannotResend(){
  val e=setup();val pool=Executors.newFixedThreadPool(2);val start=CountDownLatch(1)
  try{val futures=(1..2).map{pool.submit<Boolean>{start.await();store.reserve(e,System.currentTimeMillis())!=null}};start.countDown();assertEquals(1,futures.count{it.get(5,TimeUnit.SECONDS)});assertEquals(1,store.rules().single().count);assertEquals("sending",store.events().single().state)}finally{pool.shutdownNow()}
 }
 @Test fun stoppedRuleCannotReserveEvenIfMessageAlreadyQueued(){val e=setup();store.stop(e.ruleId);assertNull(store.reserve(e,System.currentTimeMillis()));assertEquals(0,store.rules().single().count)}
}
