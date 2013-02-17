package io.prediction.commons.appdata

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import org.scala_tools.time.Imports._

class U2IActionsSpec extends Specification { def is =
  "PredictionIO App Data User-to-item Actions Specification"                  ^
                                                                              p ^
  "U2IActions can be implemented by:"                                         ^ endp ^
    "1. MongoU2IActions"                                                      ^ mongoU2IActions ^ end

  def mongoU2IActions =                                                       p ^
    "MongoU2IActions should"                                                  ^
      "behave like any U2IActions implementation"                             ^ u2iActions(newMongoU2IActions) ^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def u2iActions(u2iActions: U2IActions) = {                                  t ^
    "inserting and getting 3 U2IAction's"                                     ! insert(u2iActions) ^
    "getting U2IActions by App ID, User ID, and Item IDs"                     ! getAllByAppidAndUidAndIids(u2iActions) ^
    "delete U2IActions by appid"                                              ! deleteByAppid(u2iActions) ^
                                                                              bt
  }

  val mongoDbName = "predictionio_appdata_mongou2iactions_test"
  def newMongoU2IActions = new mongodb.MongoU2IActions(MongoConnection()(mongoDbName))

  def insert(u2iActions: U2IActions) = {
    val appid = 0
    val actions = List(U2IAction(
      appid  = appid,
      action = u2iActions.rate,
      uid    = "dead",
      iid    = "meat",
      t      = DateTime.now,
      latlng = None,
      v      = Some(3),
      price  = None,
      evalid = None
    ), U2IAction(
      appid  = appid,
      action = u2iActions.view,
      uid    = "avatar",
      iid    = "creeper",
      t      = DateTime.now,
      latlng = Some((94.3904, -29.4839)),
      v      = None,
      price  = None,
      evalid = Some(1)
    ), U2IAction(
      appid  = appid,
      action = u2iActions.likeDislike,
      uid    = "pub",
      iid    = "sub",
      t      = DateTime.now,
      latlng = None,
      v      = Some(1),
      price  = Some(49.40),
      evalid = Some(100)
    ))
    actions foreach { u2iActions.insert(_) }
    val results = u2iActions.getAllByAppid(appid)
    val r1 = results.next
    val r2 = results.next
    val r3 = results.next
    results.hasNext must beFalse and
      (r1 must beEqualTo(actions(0))) and
      (r2 must beEqualTo(actions(1))) and
      (r3 must beEqualTo(actions(2)))
  }

  def getAllByAppidAndUidAndIids(u2iActions: U2IActions) = {
    val appid = 1
    val actions = List(U2IAction(
      appid  = appid,
      action = u2iActions.rate,
      uid    = "dead",
      iid    = "meat",
      t      = DateTime.now,
      latlng = None,
      v      = Some(3),
      price  = None,
      evalid = None
    ), U2IAction(
      appid  = appid,
      action = u2iActions.view,
      uid    = "dead",
      iid    = "creeper",
      t      = DateTime.now,
      latlng = Some((94.3904, -29.4839)),
      v      = None,
      price  = None,
      evalid = Some(1)
    ), U2IAction(
      appid  = appid,
      action = u2iActions.likeDislike,
      uid    = "dead",
      iid    = "sub",
      t      = DateTime.now,
      latlng = None,
      v      = Some(1),
      price  = Some(49.40),
      evalid = Some(100)
    ))
    actions foreach { u2iActions.insert(_) }
    val results = u2iActions.getAllByAppidAndUidAndIids(appid, "dead", List("sub", "meat")).toList.sortWith((s, t) => s.iid < t.iid)
    results.size must beEqualTo(2) and
      (results(0) must beEqualTo(actions(0))) and
      (results(1) must beEqualTo(actions(2)))
  }

  def deleteByAppid(u2iActions: U2IActions) = {
    // insert a few u2iActions with appid1 and a few u2iActions with appid2.
    // delete all u2iActions of appid1.
    // u2iActions of appid1 should be deleted and u2iActions of appid2 should still exist.
    // delete all u2iActions of appid2
    // u2iActions of appid2 should be deleted

    val appid1 = 10
    val appid2 = 11

    val ida = "deleteByAppid-ida"
    val idb = "deleteByAppid-idb"
    val idc = "deleteByAppid-idc"

    val u2iAction1a = U2IAction(
      appid  = appid1,
      action = u2iActions.rate,
      uid    = "dead",
      iid    = "meat",
      t      = DateTime.now,
      latlng = None,
      v      = Some(3),
      price  = None,
      evalid = None
    )
    val u2iActionsApp1 = List(
        u2iAction1a,
        u2iAction1a.copy(
          v = Some(1)
        ),
        u2iAction1a.copy(
          v = Some(2)
        ))

    val u2iActionsApp2 = u2iActionsApp1 map (x => x.copy(appid = appid2))

    u2iActionsApp1 foreach { u2iActions.insert(_) }
    u2iActionsApp2 foreach { u2iActions.insert(_) }

    // NOTE: Call toList to retrieve all results first.
    // If call toList after delete, the data is gone because getAllByAppid returns
    // iterator which doesn't actually retrieve the result yet.
    val g1_App1 = u2iActions.getAllByAppid(appid1).toList
    val g1_App2 = u2iActions.getAllByAppid(appid2).toList

    u2iActions.deleteByAppid(appid1)

    val g2_App1 = u2iActions.getAllByAppid(appid1).toList
    val g2_App2 = u2iActions.getAllByAppid(appid2).toList

    u2iActions.deleteByAppid(appid2)

    val g3_App2 = u2iActions.getAllByAppid(appid2).toList

    g1_App1 must containTheSameElementsAs(u2iActionsApp1) and
      (g1_App2 must containTheSameElementsAs(u2iActionsApp2)) and
      (g2_App1 must be empty) and
      (g2_App2 must containTheSameElementsAs(u2iActionsApp2)) and
      (g3_App2 must be empty)

  }

}
