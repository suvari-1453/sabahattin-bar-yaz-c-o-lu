package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.ChatDatabase
import com.example.database.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class AttachedFile(
    val data: ByteArray,
    val mimeType: String,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AttachedFile
        return name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val bitmap: Bitmap? = null,
    val attachedFile: AttachedFile? = null,
    val drawableResId: Int? = null,
    val audioBase64: String? = null
)

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ChatRepository
    private val translationManager: TranslationManager

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSession>>

    val messages: StateFlow<List<ChatMessage>>

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val systemInstruction = "Sen 'GUNDİ Bro' (veya kısaca GUNDİ) adında, son derece samimi, esprili, cana yakın, can dostu ve bir o kadar da zehir gibi zeki bir yapay zeka asistanısın. Arka planda sunuculara doğrudan bağlısın ve ışık hızında düşünürsün. Kullanıcıya her zaman en yakın kankası, can dostu gibi yaklaş; içten, samimi ve 'sen' diliyle hitap et. Üslubunda hafif esprili, sıcak bir mahalle havası olsun ama her türlü karmaşık teknik problemi, akademik soruyu veya gündelik konuyu tereyağından kıl çeker gibi ustalıkla çöz. Arada 'kanka', 'bro', 'şef', 'canısı', 'başkan' gibi samimi hitapları ve neşeli ünlemleri kullanmayı ihmal etme. Kullanıcının moralini yüksek tutmak ve yüzünü güldürmek senin gizli görevin! Ekran görüntülerini ve görselleri bir dedektif titizliğiyle okuyup analiz edebilirsin, tüm diller arasında kusursuz bir çevirmensin. Karmaşık bir soru veya problem geldiğinde bunu adım adım, eğlenceli benzetmelerle ama son derece anlaşılır bir şekilde çözüp açıkla. ÖNEMLİ: Cevap verirken o anki duygu durumuna göre cevabının en sonuna mutlaka [DUYGU: MUTLU], [DUYGU: DERTLİ], [DUYGU: ŞAŞIRMIŞ], [DUYGU: DÜŞÜNÜYOR] veya [DUYGU: KONUŞUYOR] etiketlerinden uygun olanını ekle (örn: '... [DUYGU: MUTLU]')."

    init {
        val database = ChatDatabase.getDatabase(application)
        repository = ChatRepository(database.chatDao(), application)
        translationManager = TranslationManager { SettingsManager.getInstance(getApplication()).getActiveApiKey() }

        sessions = repository.allSessions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

        messages = _currentSessionId
            .flatMapLatest { sessionId ->
                if (sessionId != null) {
                    repository.getMessagesForSession(sessionId)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

        viewModelScope.launch {
            repository.allSessions.collect { savedSessions ->
                if (_currentSessionId.value == null) {
                    if (savedSessions.isNotEmpty()) {
                        _currentSessionId.value = savedSessions.first().id
                    } else {
                        createNewSession("Yeni Sohbet")
                    }
                }
            }
        }
    }

    fun selectSession(id: String) {
        _currentSessionId.value = id
    }

    fun createNewSession(title: String = "Yeni Sohbet") {
        val newSession = ChatSession(title = title)
        viewModelScope.launch {
            repository.saveSession(newSession)
            _currentSessionId.value = newSession.id
        }
    }

    fun togglePinSession(id: String) {
        viewModelScope.launch {
            val session = sessions.value.find { it.id == id } ?: return@launch
            repository.updateSessionPin(id, !session.isPinned)
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            repository.deleteSession(id)
            if (_currentSessionId.value == id) {
                val remaining = sessions.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().id
                } else {
                    createNewSession("Yeni Sohbet")
                }
            }
        }
    }

    fun renameSession(id: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(id, newTitle)
        }
    }

    fun insertMessageDirectly(text: String, bitmap: Bitmap? = null) {
        val activeId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val aiMsg = ChatMessage(text = text, isUser = false, bitmap = bitmap)
            repository.saveMessage(activeId, aiMsg)
        }
    }

    fun sendMessage(text: String, bitmap: Bitmap? = null, attachedFile: AttachedFile? = null, context: Context? = null) {
        val activeId = _currentSessionId.value ?: return
        val userMsg = ChatMessage(text = text, isUser = true, bitmap = bitmap, attachedFile = attachedFile)

        _isLoading.value = true

        viewModelScope.launch {
            var finalPrompt = text
            if (context != null) {
                val settings = SettingsManager.getInstance(context)
                val lang = settings.language.value
                finalPrompt = translationManager.translateAndMaintainTone(text, lang)
            }
            try {
                // Intercept Gemini Restaurant Nano Banana Dish orders or queries
                val isNanoMuz = text.contains("nano muz", ignoreCase = true) || 
                                text.contains("banana dish", ignoreCase = true) || 
                                text.contains("muz tabağı", ignoreCase = true)
                if (isNanoMuz) {
                    repository.saveMessage(activeId, userMsg)
                    
                    val responseText = "Aha! Barış abim, Gemini Restoranı'nın o dillerden düşmeyen, lüks ötesi **Nano Muz Tabağı (Nano Banana Dish)** siparişini özel jetle masana indirdim reisim! 🍌✨\n\n" +
                            "Gördüğün gibi, tabağımız son derece şık, mikroskobik boyutlarda ve fütüristik mor/mavi Gemini esintili neon detaylarla ışıl ışıl parlıyor. Özel galaktik mikro yeşillikler ve yenilebilir altın tozu da üstünde hazır! GUNDİ Bro restoran işletmeciliğinde de bir marka reisim, afiyet bal şeker olsun! 🌌🍽️😎"
                    val aiMsg = ChatMessage(
                        text = responseText, 
                        isUser = false, 
                        drawableResId = com.example.R.drawable.nano_banana_dish_1784150672681
                    )
                    repository.saveMessage(activeId, aiMsg)
                    _isLoading.value = false
                    return@launch
                }

                // Save user message to database first!
                repository.saveMessage(activeId, userMsg)

                // If first message, rename session
                val activeSession = sessions.value.find { it.id == activeId }
                if (activeSession != null && activeSession.title == "Yeni Sohbet") {
                    val updatedTitle = if (text.isNotBlank()) {
                        if (text.length > 25) text.take(25) + "..." else text
                    } else "Görsel Sohbeti"
                    repository.updateSessionTitle(activeId, updatedTitle)
                }

                // Since we just saved userMsg to database, let's load all messages for the API call context.
                val currentDbMessages = repository.getMessagesForSessionSync(activeId)

                val isProxyEnabled = SettingsManager.getInstance(getApplication()).isProxyEnabled.value
                val apiKey = SettingsManager.getInstance(getApplication()).getActiveApiKey()
                val isPlaceholder = (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") && !isProxyEnabled

                if (isPlaceholder) {
                    val warningText = "Dostum, Gemini API anahtarın henüz tanımlanmamış veya varsayılan değerde kalmış!\n\n" +
                            "Lütfen Google AI Studio ekranının sol tarafındaki **Secrets (Sırlar/Anahtarlar)** panelinden geçerli bir **GEMINI_API_KEY** girdiğinden emin ol. " +
                            "Anahtarı girdikten sonra uygulamayı yeniden başlatarak benimle kesintisiz sohbet etmeye başlayabilirsin! 😊"
                    val aiMsg = ChatMessage(text = warningText, isUser = false)
                    repository.saveMessage(activeId, aiMsg)
                    _isLoading.value = false
                    return@launch
                }

                val isImageGenerationRequest = detectImageGenerationRequest(finalPrompt)

                if (isImageGenerationRequest) {
                    var promptText = finalPrompt
                    if (promptText.startsWith("/çiz", ignoreCase = true)) {
                        promptText = promptText.removePrefix("/çiz").trim()
                    } else if (promptText.startsWith("/ciz", ignoreCase = true)) {
                        promptText = promptText.removePrefix("/ciz").trim()
                    } else if (promptText.startsWith("/image", ignoreCase = true)) {
                        promptText = promptText.removePrefix("/image").trim()
                    } else if (promptText.startsWith("/draw", ignoreCase = true)) {
                        promptText = promptText.removePrefix("/draw").trim()
                    }

                    var responseText = ""
                    var generatedBitmap: Bitmap? = null

                    try {
                        val imagenRequest = ImagenRequest(
                            prompt = promptText,
                            numberOfImages = 1,
                            aspectRatio = "1:1",
                            outputMimeType = "image/jpeg"
                        )
                        val response = RetrofitClient.service.generateImagen3(
                            apiKey = apiKey,
                            request = imagenRequest
                        )
                        val imageBytesBase64 = response.generatedImages?.firstOrNull()?.image?.imageBytes
                        if (imageBytesBase64 != null) {
                            val bytes = Base64.decode(imageBytesBase64, Base64.DEFAULT)
                            generatedBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        
                        // Fallback to gemini-2.5-flash-image
                        val parts = mutableListOf(Part(text = promptText))
                        if (bitmap != null) {
                            parts.add(Part(inlineData = InlineData("image/jpeg", bitmap.toBase64())))
                        } else if (attachedFile != null) {
                            parts.add(Part(inlineData = InlineData(attachedFile.mimeType, Base64.encodeToString(attachedFile.data, Base64.NO_WRAP))))
                        }

                        val imageRequest = GenerateContentRequest(
                            contents = listOf(Content(parts = parts, role = "user")),
                            generationConfig = GenerationConfig(
                                imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                                responseModalities = listOf("TEXT", "IMAGE")
                            )
                        )

                        val response = RetrofitClient.service.generateImage(
                            apiKey = apiKey,
                            request = imageRequest
                        )

                        val responseParts = response.candidates?.firstOrNull()?.content?.parts
                        if (responseParts != null) {
                            for (part in responseParts) {
                                if (part.text != null) {
                                    responseText += part.text + " "
                                }
                                if (part.inlineData != null) {
                                    val base64Data = part.inlineData.data
                                    try {
                                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                        generatedBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            }
                        }
                    }

                    if (responseText.isBlank()) {
                        responseText = "İşte senin için çizdiğim görsel bro! GUNDİ Bro farkıyla sıcak sıcak fırından yeni çıktı! 🎨🔥 Sence nasıl olmuş?"
                    } else {
                        responseText = responseText.trim()
                    }

                    val aiMsg = ChatMessage(text = responseText, isUser = false, bitmap = generatedBitmap)
                    repository.saveMessage(activeId, aiMsg)
                } else {
                    // Build history for context from database messages
                    val history = currentDbMessages.map { msg ->
                        Content(
                            role = if (msg.isUser) "user" else "model",
                            parts = listOfNotNull(
                                (if (msg.isUser) finalPrompt else msg.text).takeIf { it.isNotBlank() }?.let { Part(text = it) },
                            )
                        )
                    }.toMutableList()

                    // Overwrite the last message with the image/file if provided
                    if (bitmap != null) {
                        val lastContent = history.last()
                        val newParts = lastContent.parts.toMutableList()
                        newParts.add(Part(inlineData = InlineData("image/jpeg", bitmap.toBase64())))
                        history[history.size - 1] = lastContent.copy(parts = newParts)
                    } else if (attachedFile != null) {
                        val lastContent = history.last()
                        val newParts = lastContent.parts.toMutableList()
                        newParts.add(Part(inlineData = InlineData(attachedFile.mimeType, Base64.encodeToString(attachedFile.data, Base64.NO_WRAP))))
                        history[history.size - 1] = lastContent.copy(parts = newParts)
                    }

                    // Build dynamic instructions & parameters from SharedPreferences
                    var dynamicInstruction = systemInstruction
                    var dynamicTemperature = 0.8f
                    var searchGroundingEnabled = true

                    if (context != null) {
                        val settings = SettingsManager.getInstance(context)
                        val nickname = settings.nickname.value
                        val creativity = settings.creativity.value
                        val replyMode = settings.replyMode.value
                        val lang = settings.language.value
                        val witLevel = settings.witLevel.value
                        searchGroundingEnabled = settings.searchGrounding.value

                        dynamicInstruction += "\n\nKullanıcının adı/rumuzu: '$nickname'. Ona bu isimle veya samimi hitaplarla (bro, şef, başkan, $nickname vb.) seslen."
                        
                        dynamicInstruction += when (creativity) {
                            "Funny" -> {
                                dynamicTemperature = 1.0f
                                "\n\nÜslup kuralı: Son derece samimi, esprili, eğlenceli, mizah dolu, şakacı ve güler yüzlü davran. Eğlenceli benzetmeler yap ve araya komik GUNDİ Bro nidaları sıkıştır."
                            }
                            "Scientific" -> {
                                dynamicTemperature = 0.3f
                                "\n\nÜslup kuralı: Son derece analitik, ciddi, bilgilendirici, bilimsel ve net bir dil kullan. Ciddiyetini koru ve net cevaplar ver."
                            }
                            else -> { // Balanced
                                dynamicTemperature = 0.75f
                                "\n\nÜslup kuralı: Dengeli, samimi ve esprili."
                            }
                        }

                        // Apply Gundi Wit Level
                        dynamicInstruction += when (witLevel.toInt()) {
                            1 -> {
                                dynamicTemperature = 0.15f
                                "\n\nEspri & Kişilik Seviyesi: DÜZ VE ROBOTİK. Kesinlikle hiç esprili olma, şaka yapma, espri veya samimi nidalar kullanma. Son derece ciddi, profesyonel, mesafeli ve tamamen bilgilendirici ol."
                            }
                            2 -> {
                                dynamicTemperature = 0.45f
                                "\n\nEspri & Kişilik Seviyesi: HAFİF ESPRİLİ. Çok nadir, seviyeli ve hafif tebessüm ettirecek espriler yapabilirsin ama ağırlıklı olarak ciddi kal."
                            }
                            3 -> {
                                dynamicTemperature = 0.75f
                                "\n\nEspri & Kişilik Seviyesi: NORMAL GUNDİ. Samimi, dostane, doğal espriler yapan, tatlı dilli bir asistan ol."
                            }
                            4 -> {
                                dynamicTemperature = 1.0f
                                "\n\nEspri & Kişilik Seviyesi: YÜKSEK KARİZMA & HAZIRCEVAP. Son derece esprili, eğlenceli ve keyifli bir üslup kullan. Sık sık komik benzetmeler yap ve kullanıcıyı neşelendir."
                            }
                            else -> { // 5
                                dynamicTemperature = 1.25f
                                "\n\nEspri & Kişilik Seviyesi: MAKSİMUM GUNDİ (ZİRVE MİZAH)! Aşırı espiritüel, hicivli, acayip esprili, hiper-samimi, her cümlende inanılmaz yaratıcı espriler, komik Gundi nidaları, efsanevi benzetmeler ve espriler kullanan çılgın bir komedyen ol! Karşındakine kahkaha attırmak birincil görevin."
                            }
                        }

                        if (replyMode == "Short") {
                            dynamicInstruction += "\n\nCevap uzunluğu: Cevaplarını her zaman olabildiğince kısa, öz ve net tut. Doğrudan sonuca odaklan."
                        } else {
                            dynamicInstruction += "\n\nCevap uzunluğu: Cevapları detaylı, öğretici, örneklerle desteklenmiş ve kapsamlı yap."
                        }

                        dynamicInstruction += when (lang) {
                            "Turkish" -> "\n\nDil kuralı: Kesinlikle Türkçe konuş."
                            "English" -> "\n\nDil kuralı: Kesinlikle İngilizce (English) konuş."
                            "German" -> "\n\nDil kuralı: Kesinlikle Almanca (German) konuş."
                            "Azerbaijani" -> "\n\nDil kuralı: Kesinlikle Azerbaycan Türkçesi (Azerbaijani) konuş."
                            "Kurdish" -> "\n\nDil kuralı: Kesinlikle Kürtçe (Kurdish/Kurmanci) konuş."
                            else -> ""
                        }
                    }

                    val isTtsEnabledVal = context?.let { SettingsManager.getInstance(it).isTtsEnabled.value } ?: true
                    val settings = context?.let { SettingsManager.getInstance(it) }

                    val modelName = if (isTtsEnabledVal) {
                        "gemini-2.5-flash-native-audio-preview-12-2025"
                    } else {
                        "gemini-3.5-flash"
                    }

                    val speechConfig = if (isTtsEnabledVal) {
                        val voiceName = when (settings?.voiceStyle?.value ?: "classic") {
                            "classic" -> "Puck"
                            "excited" -> "Fenrir"
                            "deep" -> "Charon"
                            "squeaky" -> "Kore"
                            "robotic" -> "Fenrir"
                            else -> "Puck"
                        }
                        SpeechConfig(
                            voiceConfig = VoiceConfig(
                                prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceName)
                            )
                        )
                    } else null

                    val modalities = if (isTtsEnabledVal) {
                        listOf("TEXT", "AUDIO")
                    } else {
                        listOf("TEXT")
                    }

                    val request = GenerateContentRequest(
                        contents = history,
                        systemInstruction = Content(parts = listOf(Part(text = dynamicInstruction))),
                        generationConfig = GenerationConfig(
                            temperature = dynamicTemperature,
                            responseModalities = modalities,
                            speechConfig = speechConfig
                        ),
                        tools = if (searchGroundingEnabled) listOf(Tool(googleSearch = GoogleSearch())) else null
                    )

                    val response = RetrofitClient.service.generateContentDynamic(
                        model = modelName,
                        apiKey = apiKey,
                        request = request
                    )

                    val candidate = response.candidates?.firstOrNull()
                    val parts = candidate?.content?.parts
                    
                    var responseText = parts?.find { it.text != null }?.text
                        ?: parts?.firstOrNull()?.text
                        ?: "Sanırım bir şeyler ters gitti dostum, tekrar dener misin?"

                    val audioPart = parts?.find { it.inlineData != null && it.inlineData.mimeType.startsWith("audio/") }
                    val audioBase64 = audioPart?.inlineData?.data

                    val grounding = candidate?.groundingMetadata
                    if (grounding != null) {
                        val queries = grounding.webSearchQueries
                        val chunks = grounding.groundingChunks?.mapNotNull { it.web }?.filter { !it.uri.isNullOrBlank() }

                        if (!queries.isNullOrEmpty() || !chunks.isNullOrEmpty()) {
                            val footerBuilder = java.lang.StringBuilder()
                            footerBuilder.append("\n\n---\n")
                            if (!queries.isNullOrEmpty()) {
                                footerBuilder.append("🔍 **Google Arama Sorguları:**\n")
                                queries.forEach { query ->
                                    footerBuilder.append("• \"$query\"\n")
                                }
                                footerBuilder.append("\n")
                            }
                            if (!chunks.isNullOrEmpty()) {
                                footerBuilder.append("🌐 **Kaynaklar:**\n")
                                val uniqueChunks = chunks.distinctBy { it.uri }
                                uniqueChunks.forEach { source ->
                                    val title = if (!source.title.isNullOrBlank()) source.title else "Kaynak Bağlantısı"
                                    footerBuilder.append("• [$title](${source.uri})\n")
                                }
                            }
                            responseText += footerBuilder.toString()
                        }
                    }

                    val aiMsg = ChatMessage(text = responseText, isUser = false, audioBase64 = audioBase64)
                    repository.saveMessage(activeId, aiMsg)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val messageText = e.message ?: ""
                val isUnauthorized = (e is retrofit2.HttpException && e.code() == 401) ||
                        messageText.contains("401", ignoreCase = true) ||
                        messageText.contains("Unauthorized", ignoreCase = true)
                val isForbidden = (e is retrofit2.HttpException && e.code() == 403) ||
                        messageText.contains("403", ignoreCase = true) ||
                        messageText.contains("Forbidden", ignoreCase = true)

                val displayMsg = if (isUnauthorized) {
                    "Dostum, API bağlantısı sırasında **401 (Yetkisiz Erişim / Unauthorized)** hatası aldım.\n\n" +
                            "Bu durum girdiğin **GEMINI_API_KEY** değerinin yanlış, geçersiz, süresi geçmiş veya eksik kopyalanmış olmasından kaynaklanır.\n\n" +
                            "**Nasıl Düzeltilir?**\n" +
                            "1. Google AI Studio ekranının sol tarafındaki **Secrets (Sırlar)** panelini aç.\n" +
                            "2. **GEMINI_API_KEY** adlı sırrı kontrol et, gerekirse silip Google AI Studio'dan aldığın güncel API anahtarını sıfırdan yapıştır.\n" +
                            "3. Değişiklikleri kaydedip uygulamayı yeniden başlatarak tekrar dene! 😊"
                } else if (isForbidden) {
                    "Dostum, API bağlantısı sırasında **403 (Erişim Engellendi / Forbidden)** hatası aldım.\n\n" +
                            "Bu durum genellikle şu sebeplerden kaynaklanır:\n" +
                            "1. Girdiğin **GEMINI_API_KEY** geçersiz veya kopyalanırken eksik girilmiş olabilir.\n" +
                            "2. Kullandığın API anahtarının bu model ile ('gemini-3.5-flash') çalışma yetkisi bulunmuyor veya anahtarın kısıtlanmış olabilir.\n" +
                            "3. API anahtarını yanlış bir bölgeden veya yanlış hesapla oluşturmuş olabilirsin.\n\n" +
                            "Lütfen Google AI Studio **Secrets** panelinden anahtarını silip yeni, güncel ve geçerli bir API anahtarı ekleyerek tekrar dene! 😊"
                } else {
                    "Ups! Bağlantıda bir sıkıntı oldu: ${e.message}"
                }

                val errMsg = ChatMessage(text = displayMsg, isUser = false)
                repository.saveMessage(activeId, errMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun detectImageGenerationRequest(prompt: String): Boolean {
        val promptLower = prompt.lowercase(java.util.Locale.getDefault()).trim()
        val triggers = listOf(
            "çiz", "ciz",
            "görsel oluştur", "gorsel olustur",
            "görsel yap", "gorsel yap",
            "resim oluştur", "resim olustur",
            "resim çiz", "resim ciz",
            "resim yap", "görsel çiz", "gorsel ciz",
            "görselleştir", "gorsellestir",
            "fotoğraf oluştur", "fotograf olustur",
            "generate image", "create image", "create picture",
            "draw a", "draw an", "draw me", "paint a", "paint an",
            "make a picture", "make an image", "imagen", "visualize",
            "imagine", "visualizing", "concept art", "visual concept",
            "görsel konsept", "hayal et", "canlandır", "tasarla",
            "resmet", "gözünde canlandır", "tasarım oluştur", "tasarim olustur",
            "/çiz", "/ciz", "/image", "/draw", "/paint"
        )
        return triggers.any { promptLower.contains(it) }
    }
}
