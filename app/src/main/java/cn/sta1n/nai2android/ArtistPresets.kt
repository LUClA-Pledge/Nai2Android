package cn.sta1n.nai2android

data class ArtistPreset(
    val id: String,
    val label: String,
    val value: String
)

/** The five artist presets exposed by the website's create page. */
val WEBSITE_ARTIST_PRESETS = listOf(
    ArtistPreset(
        id = "fresh",
        label = "韩漫小清新风",
        value = "masterpiece, best quality,[[[artist:dishwasher1910]]], {{yd_(orange_maru)}}, [artist:ciloranko], [artist:sho_(sho_lwlw)], [ningen mame], soft lighting,year 2024"
    ),
    ArtistPreset(
        id = "comicDoujin",
        label = "动漫同人风",
        value = "masterpiece, best quality, very aesthetic, modern Japanese anime, official anime art, anime key visual, anime screencap, soft cel shading, soft anime coloring, smooth color transitions, natural skin tones, restrained color palette, slightly desaturated, muted colors, soft ambient lighting, gentle contrast, subtle gradients, subtle bloom, detailed anime background"
    ),
    ArtistPreset(
        id = "2.5d",
        label = "2.5D唯美风",
        value = """0.9::misaka_12003-gou ::, dino_(dinoartforame), wanke, liduke, year 2025, realistic, 4k, -2::green ::, textless version, The image is highly intricate finished drawn. Only the character's face is in anime style, but their body is in realistic style. 1.35::A highly finished photo-style artwork that has lively color, graphic texture, realistic skin surface, and lifelike flesh with little obliques::. 1.63::photorealistic::, 1.63::photo(medium)::,
20::best quality, absurdres, very aesthetic, detailed, masterpiece::,, very aesthetic, masterpiece, no text,"""
    ),
    ArtistPreset(
        id = "lolita25d",
        label = "2.5D唯美风（萝）",
        value = """20::best quality, absurdres, very aesthetic, detailed, masterpiece::, 20::highly finished::, 10::ultra detailed::, 5::masterpiece::, 5::best quality::,

2.4::kidmo::, 1.2::omone hokoma agm::, 1.1::dino, wanke, liduke::, 0.8::rurudo, mignon, artist:pottsness, artist:toosaka asagi::, 0.7::misaka_12003-gou::, 0.6::artist:chocoan, artist:ciloranko, artist:rhasta, artist:sho_sho_lwlw::, dino_(dinoartforame), agoto, akakura, 0.9::rurudo(Only body shape), mignon(Only body shape) ::

year 2025, textless version, {{petite,loli}}, Petite figure, no text, The image is highly intricate finished drawn. Only the character's face is in anime style, but their body is in realistic style. 1.35::A highly finished photo-style artwork that has graphic texture, realistic skin surface, and lifelike flesh with little obliques::, smooth line, glossy skin, realistic, 4k,

1.63::photorealistic::, 1.63::photo(medium)::, 3::simple background::, 2::depth of field::,

1.5::vivid color, lively color::, desaturated, muted tones, cinematic desaturation, pale aesthetic, silver-toned,

-2::green::, -1.5::vibrant, colorful, saturated::"""
    ),
    ArtistPreset(
        id = "doujin",
        label = "本子里番风",
        value = "1.4::asanagi::,{{{{{artist:asanagi}}}}},1.2::xiaoluo_xl::,1.3::Artist: misaka_12003-gou::,1.2::Artist:shexyo::,0.7::Artist:b.sa_(bbbs)::,1::Artist:qiandaiyiyu::,1.05::artist:natedecock::,1.05::artist:kunaboto::,1.05::artist:zer0.zer0 ::,1.05::artist:jasony::,0.75::misaka_12003-gou ::, dino_(dinoartforame), wanke, liduke, year 2025, realistic, 4k, -2::green ::, {textless version, The image is highly intricate finished drawn,write realistically,true to life}, 1.35::A highly finished photo-style artwork that has graphic texture, realistic skin surface, and lifelike flesh with little obliques::, 1.63::photorealistic::,3::age slider::,1.63::photo(medium)::, 2::best quality, absurdres, very aesthetic, detailed, masterpiece::,-4::Muscle definition, abs::"
    ),
    ArtistPreset(
        id = "galgame",
        label = "GalGame风",
        value = "artist:ningen_mame,, noyu_(noyu23386566),, toosaka asagi,, location,\n20::best quality, absurdres, very aesthetic, detailed, masterpiece::,:,, very aesthetic, masterpiece, no text,"
    )
)

val DEFAULT_WEBSITE_ARTIST_PRESET: ArtistPreset = WEBSITE_ARTIST_PRESETS.first { it.id == "2.5d" }

fun findWebsiteArtistPreset(value: String): ArtistPreset? = WEBSITE_ARTIST_PRESETS.firstOrNull {
    it.value.trim() == value.trim()
}
