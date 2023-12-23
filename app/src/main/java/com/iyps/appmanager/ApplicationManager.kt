/*
 * Copyright (c) 2022-present StellarSand
 *
 *  This file is part of IYPS.
 *
 *  IYPS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  IYPS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with IYPS.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.iyps.appmanager

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.google.android.material.color.DynamicColors
import com.iyps.R
import com.iyps.inputstream.ResourceFromInputStream
import com.iyps.preferences.PreferenceManager
import com.iyps.preferences.PreferenceManager.Companion.MATERIAL_YOU
import com.iyps.preferences.PreferenceManager.Companion.THEME_PREF
import com.nulabinc.zxcvbn.StandardDictionaries
import com.nulabinc.zxcvbn.StandardKeyboards
import com.nulabinc.zxcvbn.Zxcvbn
import com.nulabinc.zxcvbn.ZxcvbnBuilder
import com.nulabinc.zxcvbn.matchers.DictionaryLoader
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

class ApplicationManager : Application() {
    
    val preferenceManager by lazy {
        PreferenceManager(this)
    }
    
    private val commonPasswordsResource by lazy {
        ResourceFromInputStream(
            ByteArrayInputStream(ByteArrayOutputStream().apply {
                resources.openRawResource(R.raw.top_200_2023_passwords).copyTo(this)
                resources.openRawResource(R.raw.other_common_passwords).copyTo(this)
            }.toByteArray())
        )
    }
    
    private val englishWordsResource by lazy {
        ResourceFromInputStream(
            ByteArrayInputStream(ByteArrayOutputStream().apply {
                resources.openRawResource(R.raw.english_words).copyTo(this)
                resources.openRawResource(R.raw.eff_unranked).copyTo(this)
            }.toByteArray())
        )
    }
    
    private val darkwebPasswordsResource by lazy {
        ResourceFromInputStream(resources.openRawResource(R.raw.darkweb))
    }
    
    private val frenchPasswordsResource by lazy {
        ResourceFromInputStream(resources.openRawResource(R.raw.richelieu_french))
    }
    
    private val azulPasswordsResource by lazy {
        ResourceFromInputStream(resources.openRawResource(R.raw.unkown_azul))
    }
    
    val zxcvbn: Zxcvbn by lazy {
        ZxcvbnBuilder()
            .dictionaries(listOf(DictionaryLoader("passwords", commonPasswordsResource).load(),
                                 DictionaryLoader("english_wikipedia", englishWordsResource).load(),
                                 DictionaryLoader("darkweb", darkwebPasswordsResource).load(),
                                 DictionaryLoader("richelieu_french", frenchPasswordsResource).load(),
                                 DictionaryLoader("unkown_azul", azulPasswordsResource).load(),
                                 StandardDictionaries.FEMALE_NAMES_LOADER.load(),
                                 StandardDictionaries.MALE_NAMES_LOADER.load(),
                                 StandardDictionaries.SURNAMES_LOADER.load(),
                                 StandardDictionaries.US_TV_AND_FILM_LOADER.load()))
            .keyboards(StandardKeyboards.loadAllKeyboards())
            .build()
    }
    
    val secureRandom: SecureRandom by lazy {
        try {
            when {
                Build.VERSION.SDK_INT >= 26 -> SecureRandom.getInstanceStrong()
                else -> SecureRandom()
            }
        }
        catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("SecureRandom algorithm not available", e)
        }
    }
    
    val passphraseWordsMap by lazy {
        val wordMap = mutableMapOf<String, String>()
        
        BufferedReader(InputStreamReader(resources.openRawResource(R.raw.eff_passphrase_words)))
            .useLines { lines ->
                lines.forEach { line ->
                    val (id, word) = line.split("\t")
                    wordMap[id] = word
                }
            }
        
        wordMap
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Theme
        when(preferenceManager.getInt(THEME_PREF)) {
            
            0 -> {
                if (Build.VERSION.SDK_INT >= 29){
                    AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                }
                else {
                    AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
                }
            }
            R.id.follow_system -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            R.id.light -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
            R.id.dark -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        }
        
        // Material you
        if (preferenceManager.getBooleanDefValFalse(MATERIAL_YOU)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        
    }
    
}