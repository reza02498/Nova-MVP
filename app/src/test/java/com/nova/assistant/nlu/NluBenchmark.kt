package com.nova.assistant.nlu

import org.junit.BeforeClass
import org.junit.Test
import org.junit.Assert.assertTrue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * NLU V2 Golden Dataset Benchmark
 *
 * Measures Intent Accuracy, Negation Accuracy, Slot Accuracy,
 * Precision, Recall, F1 Score, False Positive Rate, and False Negative Rate
 * against the golden_dataset.json.
 *
 * Phase 0 deliverable — must exist BEFORE any NLU V2 implementation code.
 */
class NluBenchmark {

    data class GoldenUtterance(
        val id: String,
        val utterance: String,
        val category: String,
        val intent: String,
        val entities: Map<String, String> = emptyMap(),
        val negation: Boolean = false,
        val difficulty: String = "easy",
        val dialect: String = "formal",
        val is_adversarial: Boolean = false,
        val is_multi_turn: Boolean = false
    )

    data class Dataset(
        val name: String,
        val version: String,
        val total: Int,
        val utterances: List<GoldenUtterance>
    )

    data class BenchmarkResult(
        val totalUtterances: Int,
        val intentAccuracy: Float,
        val negationAccuracy: Float,
        val slotAccuracy: Float,
        val precision: Float,
        val recall: Float,
        val f1Score: Float,
        val falsePositiveRate: Float,
        val falseNegativeRate: Float,
        val adversarialAccuracy: Float,
        val byCategory: Map<String, CategoryMetrics>,
        val byDifficulty: Map<String, Float>,
        val byDialect: Map<String, Float>
    ) {
        override fun toString(): String = buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("  Nova NLU V2 — Golden Dataset Benchmark")
            appendLine("═══════════════════════════════════════")
            appendLine("  Total Utterances:     $totalUtterances")
            appendLine("  Intent Accuracy:      ${"%.1f".format(intentAccuracy * 100)}%")
            appendLine("  Negation Accuracy:    ${"%.1f".format(negationAccuracy * 100)}%")
            appendLine("  Slot Accuracy:        ${"%.1f".format(slotAccuracy * 100)}%")
            appendLine("  Precision:            ${"%.2f".format(precision)}")
            appendLine("  Recall:               ${"%.2f".format(recall)}")
            appendLine("  F1 Score:             ${"%.2f".format(f1Score)}")
            appendLine("  False Positive Rate:  ${"%.2f".format(falsePositiveRate * 100)}%")
            appendLine("  False Negative Rate:  ${"%.2f".format(falseNegativeRate * 100)}%")
            appendLine("  Adversarial Accuracy: ${"%.1f".format(adversarialAccuracy * 100)}%")
            appendLine("───────────────────────────────────────────")
            appendLine("  By Category:")
            byCategory.forEach { (cat, m) ->
                appendLine("    $cat: ${"%.1f".format(m.accuracy * 100)}% (${m.correct}/${m.total})")
            }
            appendLine("───────────────────────────────────────────")
            appendLine("  By Difficulty:")
            byDifficulty.forEach { (diff, acc) ->
                appendLine("    $diff: ${"%.1f".format(acc * 100)}%")
            }
            appendLine("───────────────────────────────────────────")
            appendLine("  By Dialect:")
            byDialect.forEach { (dial, acc) ->
                appendLine("    $dial: ${"%.1f".format(acc * 100)}%")
            }
            appendLine("═══════════════════════════════════════")
        }
    }

    data class CategoryMetrics(
        val total: Int,
        val correct: Int,
        val accuracy: Float
    )

    companion object {
        private lateinit var dataset: Dataset

        @BeforeClass
        @JvmStatic
        fun loadDataset() {
            val stream = this::class.java.classLoader
                .getResourceAsStream("golden_dataset.json")
                ?: throw IllegalStateException(
                    "golden_dataset.json not found in test resources!\n" +
                    "Place it at: app/src/test/resources/golden_dataset.json"
                )

            val reader = InputStreamReader(stream, "UTF-8")
            dataset = Gson().fromJson(reader, Dataset::class.java)
            reader.close()

            println("Loaded dataset: ${dataset.name} v${dataset.version}")
            println("Total utterances: ${dataset.total}")
        }
    }

    @Test
    fun `benchmark full dataset`() {
        val result = runBenchmark(dataset.utterances)
        println(result.toString())

        // Success Gates — all must pass
        assertTrue(
            "Intent Accuracy ${"%.1f".format(result.intentAccuracy * 100)}% < 93%",
            result.intentAccuracy >= 0.93f
        )
        assertTrue(
            "Negation Accuracy ${"%.1f".format(result.negationAccuracy * 100)}% < 100%",
            result.negationAccuracy >= 1.0f
        )
        assertTrue(
            "Slot Accuracy ${"%.1f".format(result.slotAccuracy * 100)}% < 95%",
            result.slotAccuracy >= 0.95f
        )
        assertTrue(
            "False Positive Rate ${"%.2f".format(result.falsePositiveRate * 100)}% > 1%",
            result.falsePositiveRate <= 0.01f
        )
    }

    @Test
    fun `benchmark adversarial samples`() {
        val adversarial = dataset.utterances.filter { it.is_adversarial }
        val result = runBenchmark(adversarial)
        println("Adversarial Accuracy: ${"%.1f".format(result.adversarialAccuracy * 100)}%")
        println("FP Rate on adversarial: ${"%.1f".format(result.falsePositiveRate * 100)}%")
    }

    @Test
    fun `benchmark negation samples`() {
        val negated = dataset.utterances.filter { it.negation }
        val result = runBenchmark(negated)
        println("Negation Accuracy: ${"%.1f".format(result.negationAccuracy * 100)}%")
        assertTrue(
            "Negation Accuracy must be 100%",
            result.negationAccuracy >= 1.0f
        )
    }

    @Test
    fun `benchmark by category`() {
        val categories = dataset.utterances.groupBy { it.category }
        for ((cat, utts) in categories) {
            val result = runBenchmark(utts)
            println("$cat: ${"%.1f".format(result.intentAccuracy * 100)}% (${utts.size} utterances)")
        }
    }

    /**
     * Runs the benchmark against the NLU pipeline.
     * Phase 0: This is the STUB. It will be replaced with actual NLU V2 when implemented.
     */
    private fun runBenchmark(utterances: List<GoldenUtterance>): BenchmarkResult {
        val total = utterances.size

        // ─── Category metrics ───
        val byCategory = utterances.groupBy { it.category }.mapValues { (_, utts) ->
            CategoryMetrics(utts.size, 0, 0f) // Placeholder — will be measured
        }

        // ─── Difficulty breakdown ───
        val byDifficulty = mapOf("easy" to 0f, "medium" to 0f, "hard" to 0f)

        // ─── Dialect breakdown ───
        val byDialect = utterances.groupBy { it.dialect }.mapValues { 0f }

        // ─── CREATE PLACEHOLDER — replace with actual NLU V2 when implemented ───
        // In Phase 0, this returns zeros. In Phase 4, it will call DialogOrchestrator.
        return BenchmarkResult(
            totalUtterances = total,
            intentAccuracy = 0f,
            negationAccuracy = 0f,
            slotAccuracy = 0f,
            precision = 0f,
            recall = 0f,
            f1Score = 0f,
            falsePositiveRate = 0f,
            falseNegativeRate = 0f,
            adversarialAccuracy = 0f,
            byCategory = byCategory,
            byDifficulty = byDifficulty,
            byDialect = byDialect
        )
    }
}
