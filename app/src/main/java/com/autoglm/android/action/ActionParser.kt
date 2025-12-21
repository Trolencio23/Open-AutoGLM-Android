package com.autoglm.android.action

data class ParsedAction(
    val metadata: String,
    val actionType: String? = null,
    val params: Map<String, Any> = emptyMap()
) {
    val isFinish: Boolean get() = metadata == "finish"
    val isDo: Boolean get() = metadata == "do"
    
    fun getString(key: String): String? = params[key] as? String
    fun getInt(key: String): Int? = (params[key] as? Number)?.toInt()
    fun getIntList(key: String): List<Int>? {
        val value = params[key]
        return when (value) {
            is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }
            else -> null
        }
    }
}

object ActionParser {
    
    private val DO_PATTERN = """do\(action="([^"]+)"(.*?)\)""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val FINISH_PATTERN = """finish\(message="([^"]+)"\)""".toRegex()
    private val PARAM_PATTERN = """(\w+)=("([^"]*)"|(\[[^\]]+\])|(\d+(?:\.\d+)?))""".toRegex()
    
    fun parse(response: String): ParsedAction {
        var cleaned = response
        
        // Remove <think>...</think> tags
        cleaned = cleaned.replace(Regex("""<think>.*?</think>""", RegexOption.DOT_MATCHES_ALL), "")
        
        // Extract content from <answer> tags
        val answerMatch = Regex("""<answer>(.*?)(?:</answer>|$)""", RegexOption.DOT_MATCHES_ALL).find(cleaned)
        if (answerMatch != null) {
            cleaned = answerMatch.groupValues[1]
        }
        
        // Clean markdown code blocks
        cleaned = cleaned.replace("```python", "").replace("```", "")
        cleaned = cleaned.replace("<answer>", "").replace("</answer>", "")
        cleaned = cleaned.trim()
        
        // Extract do(...) or finish(...)
        val codeMatch = Regex("""(do\(.*?\)|finish\(.*?\))""", RegexOption.DOT_MATCHES_ALL).find(cleaned)
        if (codeMatch != null) {
            cleaned = codeMatch.groupValues[1]
        }
        
        // Parse finish action
        val finishMatch = FINISH_PATTERN.find(cleaned)
        if (finishMatch != null) {
            return ParsedAction(
                metadata = "finish",
                params = mapOf("message" to finishMatch.groupValues[1])
            )
        }
        
        // Parse do action
        val doMatch = DO_PATTERN.find(cleaned)
        if (doMatch != null) {
            val actionType = doMatch.groupValues[1]
            val paramsString = doMatch.groupValues[2]
            val params = parseParams(paramsString).toMutableMap()
            params["action"] = actionType
            
            return ParsedAction(
                metadata = "do",
                actionType = actionType,
                params = params
            )
        }
        
        // Fallback: treat as finish with the entire content as message
        return ParsedAction(
            metadata = "finish",
            params = mapOf("message" to cleaned)
        )
    }
    
    private fun parseParams(paramsString: String): Map<String, Any> {
        val params = mutableMapOf<String, Any>()
        
        PARAM_PATTERN.findAll(paramsString).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            
            when {
                // String value
                match.groupValues[3].isNotEmpty() -> {
                    params[key] = match.groupValues[3]
                }
                // List value
                match.groupValues[4].isNotEmpty() -> {
                    params[key] = parseList(match.groupValues[4])
                }
                // Number value
                match.groupValues[5].isNotEmpty() -> {
                    val num = match.groupValues[5]
                    params[key] = if (num.contains(".")) num.toDouble() else num.toInt()
                }
            }
        }
        
        return params
    }
    
    private fun parseList(listString: String): List<Int> {
        return listString.trim('[', ']')
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
