package com.y9vad9.starix.bot.fsm.admin.settings.club_settings

import com.y9vad9.starix.bot.ext.asTelegramUserId
import com.y9vad9.starix.bot.fsm.FSMState
import com.y9vad9.starix.bot.fsm.admin.AdminMainMenuState
import com.y9vad9.starix.bot.fsm.common.CommonInitialState
import com.y9vad9.starix.bot.fsm.getCurrentStrings
import com.y9vad9.starix.core.brawlstars.entity.club.value.ClubTag
import com.y9vad9.starix.core.system.usecase.settings.admin.club.ChangeClubMultiplayersSettingUseCase
import com.y9vad9.starix.core.system.usecase.settings.admin.club.GetClubSettingsUseCase
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.buttons.reply.simpleReplyButton
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("AdminManageMultiplayerSettingState")
@Serializable
data class AdminManageMultiplayerSettingState(
    override val context: IdChatIdentifier,
    val clubTag: ClubTag,
) : FSMState<AdminManageMultiplayerSettingState.Dependencies> {
    override suspend fun BehaviourContext.before(
        previousState: FSMState<*>,
        dependencies: Dependencies,
    ): FSMState<*> = with(dependencies) {
        val strings = getCurrentStrings(context)

        return when (val result = getClubSettings.execute(clubTag)) {
            GetClubSettingsUseCase.Result.ClubNotFound -> {
                bot.send(
                    chatId = context,
                    text = strings.clubNotFoundMessage,
                )
                AdminMainMenuState(context)
            }

            is GetClubSettingsUseCase.Result.Success -> {
                bot.send(
                    chatId = context,
                    entities = strings.admin.settings.multiplePlayersMessage(result.clubSettings),
                    replyMarkup = replyKeyboard {
                        val actionButtonText = if (result.clubSettings.multipleAccountsEnabled) {
                            strings.disableChoice
                        } else {
                            strings.enableChoice
                        }
                        row(simpleReplyButton(actionButtonText))
                        row(simpleReplyButton(strings.goBackChoice))
                    },
                )
                this@AdminManageMultiplayerSettingState
            }
        }
    }

    override suspend fun BehaviourContextWithFSM<in FSMState<*>>.process(
        dependencies: Dependencies,
    ): FSMState<*> = with(dependencies) {
        val strings = getCurrentStrings(context)

        return when (val text = waitText().first().text) {
            strings.enableChoice, strings.disableChoice -> {
                val result = changeClubMultiplayersSetting.execute(
                    id = context.asTelegramUserId(),
                    clubTag = clubTag,
                    status = text == strings.enableChoice,
                )

                when (result) {
                    ChangeClubMultiplayersSettingUseCase.Result.ClubNotFound -> {
                        bot.send(
                            chatId = context,
                            text = strings.clubNotFoundMessage,
                        )
                        return@with AdminMainMenuState(context)
                    }

                    ChangeClubMultiplayersSettingUseCase.Result.NoPermission -> {
                        bot.send(
                            chatId = context,
                            text = strings.noPermissionMessage,
                        )
                        return@with CommonInitialState(context)
                    }

                    ChangeClubMultiplayersSettingUseCase.Result.Success -> {
                        bot.send(
                            chatId = context,
                            text = strings.admin.settings.successfullyChangedOption,
                        )
                        AdminViewClubSettingsState(context, clubTag)
                    }
                }
            }

            strings.goBackChoice -> AdminViewClubSettingsState(context, clubTag)

            else -> {
                bot.send(
                    chatId = context,
                    text = strings.invalidChoiceMessage,
                )
                this@AdminManageMultiplayerSettingState
            }
        }
    }

    interface Dependencies : FSMState.Dependencies {
        val getClubSettings: GetClubSettingsUseCase
        val changeClubMultiplayersSetting: ChangeClubMultiplayersSettingUseCase
    }
}