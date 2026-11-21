#include <jni.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <cdiscord.h>

#define CRYSTAL_DISCORD_APP_ID UINT64_C(1571571651093733446)

static Discord_Client discord_client;
static Discord_Activity discord_activity;

static bool discord_initialized = false;

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_DiscordPresence_nativeInitialize(
        JNIEnv *env,
        jclass clazz) {

    if (discord_initialized) {
        return;
    }

    Discord_Client_Init(&discord_client);
    Discord_Client_SetApplicationId(
        &discord_client,
        CRYSTAL_DISCORD_APP_ID
    );

    Discord_Activity_Init(&discord_activity);

    discord_initialized = true;
    Discord_Client_Connect(&discord_client);
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_DiscordPresence_nativeDisconnect(
        JNIEnv *env,
        jclass clazz) {

    if (!discord_initialized) {
        return;
    }

    Discord_Client_Disconnect(&discord_client);
    Discord_Activity_Drop(&discord_activity);
    Discord_Client_Drop(&discord_client);

    discord_initialized = false;
}
