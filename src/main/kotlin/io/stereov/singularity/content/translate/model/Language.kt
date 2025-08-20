package io.stereov.singularity.content.translate.model

import com.fasterxml.jackson.annotation.JsonCreator
import io.stereov.singularity.content.translate.exception.model.TranslationLanguageNotImplementedException
import java.util.*

enum class Language {
    AA, AB, AE, AF, AK, AM, AN, AR, AS, AV, AY, AZ, BA, BE, BG, BH, BI, BM, BN, BO, BR, BS, CA, CE,
    CH, CO, CR, CS, CU, CV, CY, DA, DE, DV, DZ, EE, EL, EN, EO, ES, ET, EU, FA, FF, FI, FJ, FO, FR,
    FY, GA, GD, GL, GN, GU, GV, HA, HE, HI, HO, HR, HT, HU, HY, HZ, IA, ID, IE, IG, II, IK, IN, IO,
    IS, IT, IU, IW, JA, JI, JV, KA, KG, KI, KJ, KK, KL, KM, KN, KO, KR, KS, KU, KV, KW, KY, LA, LB,
    LG, LI, LN, LO, LT, LU, LV, MG, MH, MI, MK, ML, MN, MO, MR, MS, MT, MY, NA, NB, ND, NE, NG, NL,
    NN, NO, NR, NV, NY, OC, OJ, OM, OR, OS, PA, PI, PL, PS, PT, QU, RM, RN, RO, RU, RW, SA, SC, SD,
    SE, SG, SI, SK, SL, SM, SN, SO, SQ, SR, SS, ST, SU, SV, SW, TA, TE, TG, TH, TI, TK, TL, TN, TO,
    TR, TS, TT, TW, TY, UG, UK, UR, UZ, VE, VI, VO, WA, WO, XH, YI, YO, ZA, ZH, ZU;

    override fun toString(): String {
        return super.toString().lowercase()
    }

    fun toLocale(): Locale {
        return Locale.of(this.toString())
    }

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String): Language {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw TranslationLanguageNotImplementedException(value)
        }
    }
}
