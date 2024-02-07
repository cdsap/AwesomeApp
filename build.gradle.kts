import io.github.cdsap.talaiot.metrics.Metrics

plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.9.10") apply false
    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false
    id("io.github.cdsap.talaiot") version "2.0.3"
}

talaiot {
    publishers {
        influxDbPublisher {
            dbName = "demo"
            buildMetricName = "buildMetric"
            taskMetricName = "taskMetric"
            url = System.getenv("INFLUX1_URL")
            buildTags = listOf(io.github.cdsap.talaiot.metrics.BuildMetrics.RequestedTasks)
            taskTags = listOf(
                io.github.cdsap.talaiot.metrics.TaskMetrics.Task,
                io.github.cdsap.talaiot.metrics.TaskMetrics.Module,
                io.github.cdsap.talaiot.metrics.TaskMetrics.State
            )
        }
    }
    metrics {
        customBuildMetrics(
            "CI" to System.getenv("CI"),
        )
        customTaskMetrics(
            "CI" to System.getenv("CI")
        )
    }
}
