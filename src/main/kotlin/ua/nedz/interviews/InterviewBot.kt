package ua.nedz.interviews

import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Locality
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ua.nedz.interviews.InterviewState.PENDING
import ua.nedz.interviews.InterviewState.STARTED
import java.io.File
import kotlin.random.Random


fun main() {
    ApiContextInitializer.init()
    val botsApi = TelegramBotsApi()

    val questionsList = readQuestions("ArhipovQuestions")
    val (botUsername, botToken, creatorId) = readConfiguration("Configuration")

    try {
        botsApi.registerBot(InterviewBot(questionsList, botUsername, botToken, creatorId))
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
}


class InterviewBot(private val questionsList: List<String>,
                   private val botUserName: String,
                   private val token: String,
                   private val creatorId: Int): AbilityBot(token, botUserName) {


    private val users = mutableMapOf<Int, InterviewState>()
    private val questions = mutableMapOf<Int, MutableList<String>>()
    private val random = Random(System.nanoTime())

    fun startTheInterview(): Ability =
            Ability
                    .builder()
                    .name("interview")
                    .locality(Locality.ALL)
                    .privacy(Privacy.PUBLIC)
                    .action { ctx: MessageContext ->
                        val id = ctx.user().id
                        if (users.containsKey(id))
                            when (users[id]) {
                                PENDING -> doStartTheInterview(id, ctx)
                                STARTED -> silent.send("Interview has already started.", ctx.chatId())
                            }
                        else
                            doStartTheInterview(id, ctx)
                    }
                    .build()

    fun endTheInterview(): Ability =
            Ability
                    .builder()
                    .name("end")
                    .locality(Locality.ALL)
                    .privacy(Privacy.PUBLIC)
                    .action { ctx: MessageContext ->
                        val id = ctx.user().id
                        users[id] = PENDING
                        silent.send("Thanks for participating in this interview.", ctx.chatId())
                    }
                    .build()

    fun nextQuestion(): Ability =
            Ability
                    .builder()
                    .name("next")
                    .locality(Locality.ALL)
                    .privacy(Privacy.PUBLIC)
                    .action { ctx: MessageContext ->
                        val id = ctx.user().id
                        if (users.containsKey(id))
                            when (users[id]) {
                                PENDING -> silent.send("Firstly, you have to start the interview with command /interview", ctx.chatId())
                                STARTED -> {
                                    val interviewQuestions = questions[id]
                                    if (!interviewQuestions.isNullOrEmpty()) {
                                        val index = random.nextInt(interviewQuestions.size)
                                        val question = interviewQuestions[index]
                                        silent.send(question, ctx.chatId())
                                        interviewQuestions.remove(question)
                                    } else
                                        silent.send("Sorry, no questions left for you :(", ctx.chatId())
                                }
                            }
                        else
                            silent.send("Firstly, you have to start the interview with command /interview", ctx.chatId())
                    }
                    .build()

    private fun doStartTheInterview(id: Int, ctx: MessageContext) {
        users[id] = STARTED
        questions[id] = questionsList.toMutableList()
        silent.send("Hello! Your interview has officially started. Request for next questions using /next command!", ctx.chatId())
    }

    override fun getBotUsername(): String = botUserName

    override fun getBotToken(): String = token

    override fun creatorId(): Int = creatorId
}

data class Configuration(val userName: String, val token: String, val creatorId: Int)

fun readQuestions(fileName: String) = File(fileName).useLines { it.toMutableList() }

fun readConfiguration(fileName: String): Configuration {
    val configLines = File(fileName).useLines { it.toList() }
    return Configuration(configLines[0], configLines[1], Integer.parseInt(configLines[2]))
}

enum class InterviewState {
    PENDING,
    STARTED
}