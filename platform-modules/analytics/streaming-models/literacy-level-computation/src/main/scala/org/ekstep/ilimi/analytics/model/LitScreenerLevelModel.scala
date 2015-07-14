package org.ekstep.ilimi.analytics.model

import scala.collection.mutable.Buffer
import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods.parse
import org.ekstep.ilimi.analytics.util.CommonUtil
import org.ekstep.ilimi.analytics.EventSessionization
import org.ekstep.ilimi.analytics.streaming.LitScreenerLevelComputation
import org.ekstep.ilimi.analytics.dao.LitScreenerLevelDAO
import scala.collection.mutable.ListBuffer
import java.io.FileWriter

case class Events(events: Array[Event]);

object LitScreenerLevelModel extends Serializable {

    def compute(input: String, output: Option[String], outputDir: Option[String], location: String, parallelization: Int) {

        val validEvents = Array("OE_ASSESS");
        @transient val sc = CommonUtil.getSparkContext(parallelization, "GameEffectiveness");
        val loltMapping = EventSessionization.broadcastMapping("src/main/resources/lo_lt_mapping.csv", sc);
        val ltloMapping = EventSessionization.reverseBroadcastMapping("src/main/resources/lo_lt_mapping.csv", sc);
        val ldloMapping = EventSessionization.broadcastMapping("src/main/resources/ld_lo_mapping.csv", sc);
        val loldMapping = EventSessionization.reverseBroadcastMapping("src/main/resources/ld_lo_mapping.csv", sc);
        loldMapping.value.foreach(f => Console.println("Key:" + f._1 + " | Value:" + f._2));
        val compldMapping = EventSessionization.broadcastMapping("src/main/resources/composite_ld_mapping.csv", sc);
        val litLevelsMap = EventSessionization.broadcastLevelRanges("src/main/resources/lit_scr_level_ranges.csv", sc);
        val userMapping = LitScreenerLevelDAO.getUserMapping();
        val filePath = outputDir.getOrElse("user-aggregates") + "/sprint2_tumkur_consolidated.csv";
        writeHeader(filePath);
            
        val resultOutput = output.getOrElse("console");
        val rdd = sc.textFile(CommonUtil.getPath("s3_input_bucket", input, location), parallelization).cache();
        val events = rdd.map { line =>
            {
                implicit val formats = DefaultFormats;
                parse(line).extract[Events]
            }
        }.map { le => le.events }.reduce((a, b) => a ++ b);

        events.groupBy { event => event.uid.get }.foreach(f => {
            val events = f._2.toBuffer;
            val levelSetEvents = LitScreenerLevelComputation.compute(events, loltMapping, ldloMapping, compldMapping, litLevelsMap, resultOutput, outputDir, null);
            val filterEvents = events.distinct.filter { x => ((x.eid.equals("OE_ASSESS") || x.eid.equals("OE_INTERACT")) && x.gdata.id.equals("org.ekstep.lit.scrnr.kan.basic")) };
            val uid = userMapping.getOrElse(f._1, f._1);
            var records = new ListBuffer[Array[String]];
            // Write to CSV & upload to S3
            filterEvents.foreach { event =>
                var ltCode = "";
                var loCode = "";
                var ldCode = "";
                event.eid.getOrElse("") match {
                    case "OE_ASSESS" =>
                        ltCode = getString(event.edata.eks.qid.get.split('.')(3));
                        loCode = ltloMapping.value.getOrElse(ltCode, "").toString();
                        ldCode = loldMapping.value.getOrElse(loCode, "").toString();
                    case "OE_INTERACT" =>
                        ;
                }
                records += Array(
                    getString(uid),
                    "",
                    getString(event.eid),
                    getString(event.gdata.id),
                    getString(event.edata.eks.subj),
                    getString(ldCode),
                    getStringFromArray(event.edata.eks.mc),
                    getString(ltCode),
                    getString(event.edata.eks.qid),
                    getString(event.edata.eks.qtype),
                    getString(event.edata.eks.qlevel),
                    "",
                    getString(event.edata.eks.pass),
                    getStringFromArray(event.edata.eks.mmc),
                    getStringFromInt(event.edata.eks.score),
                    getStringFromInt(event.edata.eks.maxscore),
                    getStringFromArray(event.edata.eks.res),
                    getStringFromArray(event.edata.eks.exres),
                    getStringFromDouble(event.edata.eks.length),
                    getStringFromInt(event.edata.eks.atmpts),
                    getStringFromInt(event.edata.eks.failedatmpts),
                    getString(event.edata.eks.category),
                    getString(event.edata.eks.current),
                    getString(event.edata.eks.`type`),
                    getString(event.edata.eks.id),
                    getString(event.ts));
            }

            levelSetEvents.foreach(f => {
                records += Array(
                    getString(uid),
                    "",
                    "LEVEL_SET",
                    getString("org.ekstep.lit.scrnr.kan.basic"),
                    getString("LIT"),
                    getString(""),
                    getString(f._2),
                    getString(""),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    getStringFromInt(Option(f._3)),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    getString(Option(f._4)),
                    "",
                    "",
                    "")
            })
            val fw = new FileWriter(filePath, true);
            records.foreach { f => fw.write(f.mkString(",") + "\n"); }
            fw.close();
        });
    }

    def writeHeader(filePath: String) {
        val header = Array(
                    "Child Genie id",
                    "Location",
                    "Event ID",
                    "Game ID",
                    "Subj",
                    "Concept (Dimensions)",
                    "Micro Concept (Learning Objective)",
                    "Task Code",
                    "qid",
                    "qtype",
                    "qlevel",
                    "qtech",
                    "pass",
                    "mmc",
                    "score",
                    "maxscore",
                    "res",
                    "exres",
                    "length",
                    "atmpts",
                    "failedatmpts",
                    "category",
                    "current",
                    "type",
                    "id",
                    "ts");
        val fw = new FileWriter(filePath, true);
        fw.write(header.mkString(",") + "\n");
        fw.close();
    }

    def getString(str: String): String = {
        str.filter(_ >= ' ');
    }

    def getStringFromArray(str: Option[Array[String]]): String = {
        str.getOrElse(Array()).mkString(",").filter(_ >= ' ');
    }

    def getString(str: Option[String]): String = {
        str.getOrElse("").filter(_ >= ' ');
    }

    def getStringFromInt(i: Option[Int]): String = {
        if (i.isEmpty) {
            return "";
        }
        i.get.toString();
    }

    def getStringFromDouble(db: Option[Double]): String = {
        db.getOrElse(0).toString();
    }

}