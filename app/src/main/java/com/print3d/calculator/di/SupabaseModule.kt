package com.print3d.calculator.di

import android.content.Context
import com.print3d.calculator.BuildConfig
import com.print3d.calculator.data.auth.AuthSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient =
        createSupabaseClient(
            // Fallbacks keep the client constructable before real credentials are set in
            // local.properties; auth calls simply fail until valid values are provided.
            supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { "https://placeholder.supabase.co" },
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "placeholder-anon-key" }
        ) {
            install(Auth) {
                sessionManager = AuthSessionManager(context)
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
        }

    @Provides
    @Singleton
    fun provideAuth(client: SupabaseClient): Auth = client.auth

    @Provides
    @Singleton
    fun providePostgrest(client: SupabaseClient): Postgrest = client.postgrest
}
