import { ChartNoAxesCombined, HandCoins, Rocket, Scale, ShieldCheck } from 'lucide-vue-next'

import typeStable from '@/assets/images/characters/types/type-stable.png'
import typeStabilitySeeking from '@/assets/images/characters/types/type-stability-seeking.png'
import typeRiskNeutral from '@/assets/images/characters/types/type-risk-neutral.png'
import typeActive from '@/assets/images/characters/types/type-active.png'
import typeAggressive from '@/assets/images/characters/types/type-aggressive.png'

export const investmentTypeMeta = {
  든든지킴형: {
    character: typeStable,
    icon: ShieldCheck,
    accentClass: 'text-[#80D39C]',
  },
  차곡성장형: {
    character: typeStabilitySeeking,
    icon: HandCoins,
    accentClass: 'text-[#FFABEC]',
  },
  균형설계형: {
    character: typeRiskNeutral,
    icon: Scale,
    accentClass: 'text-[#80C7F1]',
  },
  적극성장형: {
    character: typeActive,
    icon: ChartNoAxesCombined,
    accentClass: 'text-[#FFB231]',
  },
  과감도전형: {
    character: typeAggressive,
    icon: Rocket,
    accentClass: 'text-[#EF4744]',
  },
}
