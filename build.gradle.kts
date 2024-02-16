import com.google.cloud.bigquery.InsertAllRequest
import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.entities.TaskMessageState
import io.github.cdsap.talaiot.metrics.Metrics
import io.github.cdsap.talaiot.publisher.Publisher

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.cloud:google-cloud-bigquery:2.7.0")

    }
}
plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.9.10") apply false
    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false
    id("io.github.cdsap.talaiot") version "2.0.3"
}

talaiot {
    publishers {
        jsonPublisher = true
        customPublishers(BiqQueryPublisher())
    }
}

data class ModuleDuration(
    val module: String,
    val executed: Boolean,
    val duration: Long,
    val durationModule: Long,
    val modulesExecuted: Int
)

class BiqQueryPublisher : Publisher {
    override fun publish(report: ExecutionReport) {
        val duration = report.durationMs
        val filePath = "./key.json"
        if (File(filePath).exists()) {
            val db = "mobile_build_metrics"
            val tableName = "builds5"
            var modulesUpdated = 0
            report.tasks?.groupBy { it.module }?.forEach {
                if (it.value.any { it.state == io.github.cdsap.talaiot.entities.TaskMessageState.EXECUTED }) {
                    modulesUpdated++
                }
            }

            val metricsByModule = metricsByModule(report, duration, modulesUpdated)
            val rows = mapRow(metricsByModule)
            val table =
                com.google.cloud.bigquery.TableId.of(db, tableName)
            val client = com.google.cloud.bigquery.BigQueryOptions.newBuilder()
                .setCredentials(
                    com.google.auth.oauth2.GoogleCredentials.fromStream(
                        java.io.FileInputStream(
                            filePath
                        )
                    )
                )
                .build()
                .service
            try {
                val insertRequestBuilder = InsertAllRequest.newBuilder(table)
                for (row in rows) {
                    insertRequestBuilder.addRow(row)
                }
                val response = client.insertAll(insertRequestBuilder.build())
                if (response.hasErrors()) {
                    response.insertErrors.forEach { (t, u) -> println("Response error for key: $t, value: $u") }
                } else {
                    println("Build information published to BigQuery")
                }

            } catch (e: com.google.cloud.bigquery.BigQueryException) {
                println("Insert operation not performed $e")
            }
        } else {
            println("Credentials $filePath not found")
        }
    }

    private fun metricsByModule(report: ExecutionReport, duration: String?, modulesUpdated: Int):
        List<ModuleDuration> {
        val metricsByModule = mutableListOf<ModuleDuration>()
        report.tasks!!.groupBy { it.module }.forEach {
            metricsByModule.add(
                ModuleDuration(
                    it.key,
                    it.value.any { it.state == TaskMessageState.EXECUTED },
                    duration?.toLong()!!,
                    it.value.sumOf { it.ms },
                    modulesUpdated
                )
            )
        }
        return metricsByModule
    }

    private fun mapRow(durations: List<ModuleDuration>): List<Map<String, Any>> {
        val rows = mutableListOf<Map<String, Any>>()
        durations.forEach {
            val rowContent = mutableMapOf<String, Any>()
            rowContent["module"] = it.module
            rowContent["duration"] = it.duration
            rowContent["executed"] = it.executed
            rowContent["durationModule"] = it.durationModule
            rowContent["modulesExecuted"] = it.modulesExecuted
            rows.add(rowContent)
        }
        return rows
    }
}
