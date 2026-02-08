package com.dragonminez.common.hair;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.Character;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class HairManager {
    private static final String[] DEFAULT_HAIR_RACES = {"human", "saiyan"};
    private static final Map<Integer, String> PRESET_CODES = new HashMap<>();
	private static final Map<Integer, CustomHair> PRESET_CACHE = new HashMap<>();

	private static final String CODE_PREFIX_V1 = "DMZ_HAIR:";
	private static final String CODE_PREFIX_V2 = "DMZHair_v2:";
	private static final String CODE_PREFIX_V3 = "DMZHair_v3:";
	private static final String CODE_PREFIX_V4 = "DMZ4:"; // Formato optimizado v4

	private static final String CODE_PREFIX_FULL = "DMZHairFull_v3:";
	private static final String CODE_PREFIX_FULL_V4 = "DMZF4:"; // Full set optimizado v4
	private static final String FULL_SET_SEPARATOR = "\\|";
	private static final String FULL_SET_JOINER = "|";

    static {
        initializeDefaultPresets();
    }

    private static void initializeDefaultPresets() {
		// Goku
		registerPreset(1, "DMZF4:DtWmuQFNUV7tldeSwEgYABQxBR4gt57G4QrMj09JZCxMKK4iNYGMnIImQR3EXtboPBJJgihYYor11YWCRKFnkoINEg7swAirAQeQQQgppImYQyCWpZGhND7txzzr2nb3fPLqlKJT_yw3Lcuec75557Xt8ZSy2r1Eo4HcU_pZZldTnHKqpxKyssS3yo9ZIZK5H_4Cdnyw9pPye-KrYSU8TZu8W_q638fxpCdXX1XKjGT_VTQpPaLJTVQlVxQiHzHG3e5GghdpXO6uw9cQqqqqpirZrSZiF2_6mtWeVoq77Lz9a6yZaWfaBgXsteJYRnq_FsUSzuZvmXGt-ZoRRMa9VFJJTTQveehVXTuVXiLL5X5NkZBm6hs_epsyJwb8oHbkc4-0zV5Ly9XqpRGp72s6dDN0gMNKzC0KNwiha6MiCkNWULaRrE75R283EgpUeeOfM5SJ9G6dw8kt6lpAcH3tRL0Vm8XI18Hjw7xDAP7hR9dqhxFhzMjIkSKjOc1rtuCUhvKq_g5gkhFe2JchRKoFB5eVlQKBshVKGExOOOyz9uV1JZH3SjWTTSfiZDeI0Kb34wyjERxIPRLXV6Jn4WTCPl8qyGe6LAESgRp9XZJ-OcpgwHR3DDdfQsCBqOr8IeN9svrHKhkUZwlkuDSqEb6hSDYY-wqFXdEdddbOgucN3G8DstCUgXvmUdni2JPatx6-PORtxgaeQN0m62n3n_qBssCzot26YHa2i7kNa0vFWhiMutUELiwwP5sBQJdl0-wUrkJakGufkaFKxTmHLMGGe0xFXRzcU3x4iLAMBw9FPKLFnUeZJAZS1oTlbhYRlm17SKDDwM87bhweFAwytWeB2soslpq_1FQ64dOmToUOG8G6j1sAjRlz4d6sFU5B1V5K1dQSdiQVQvWeM55PqU4fq89OtxZT_i7O6gJjri5Qop2GOYh2EVZZX2WEug7fkpO3xkr9EZG-ON0UL74uoLthPf0YPWr43WE-pXqvUwoTeUUAcrca_VrvL-2pnTp-Un5JtpQu76PzQhb5lb8l-ZkIU7F_c_DgFR-skiQ-UPzr8vaOf2iuiZWcA0DHDg_WdeuFrC2D8edh5OJcufke-U6W__IWhWctHXe0ngzEcTx0fN1QJvSZ-jUBmXPlxmjAiVYy-Q0snbb51bcNIWMFsuPREeQiWMvXVPXwnTrGGqg8lif7_qMjBCPBTA3D_oHQjzrUs7AV7T5d2gsBbPXAiVp9v4i6Pmc2HWpvXXgVljn1gJbbnH1ocB-IeEF7qmLfxXcHQXeMl5KwCvqXhp5H2FrzVM9FQv7tvQ3BtgPru4S3C0zPvaiPhJxz80Jn_CO_LyRjBr1LmNgPfYp7uDeJltY14M4uUDx2AHhLfwqZTEGznjrq_CCChcDHiPX2BLb9FX_Jo1wUHPGSBhhNA3-9Wa4VerzhIh6Qzp0fL870C6dPEMThzSXmp4xwYopas-WBAMqdyIU3cW5Co1bjM6VJEWr7li9Fe4b9KyjBshpWhMwqAxNmSZqLu9P6-D6iIyx6AxRufQRuhG-r7MWsZn8rEQzWcykOqMbmT12Vb5TF7TWfMZXTHPhs-UX9J01nwm-3bva0J8BucRWVPSbvLAuP4Q3qo8PDR0lIRpVumCh_3srtN3mc_yNXPQQWAbU9-1MUu85PKnfwtBgjq9XPGhraCh-oprIVq6_eQqI5MSww3TJ8hyLSyGZxbAq5sehItjivrJ26Az-KTKz4nQR7wRcdRudt0Rg9oN6_lUsKClHnnyN0blCFI7Ib30rW8FOJ5OilyDrLwiySbK3Bdt_6dNDxZkfcKI9CVNwWk6n1sFeaAQGos1RY_gs2snxhDCuQ01IFRz5Vq47mfX_wik50uXCDt18sYSwp__ohKuC51RSK-aON6Ubp0ZQlOPYoYsi2OZIVxBPBT2cp5tJjNc9-xbRrtfJV0tdL8sKyYjWLokGxTRTb4x4nmjQSwA3R51Rj-5Ec2yP3n3UjBLx0-QRhKeCCSszW5GZRB17uz1-_sZ19QZWW_gwbuIa34kn0Ndkxc_6vu6T4dYKEQJZ6H1ssGKB1pz8g4wQty3NRaKUeJnsE_7tmpg8SwUElzYSfHIUiCWha5c3RWE1su6E_D5_1nof4SFdo5hoRkMPTa50vOz-aXtLJS6Fp9741ho9lDF7-GZevU5etYsNKVaqMlCcbBJezReMTp6att7RmybdDQ75NFl5mQcR0fzcQtCq2VtDAi1Rke91OBXb4S4EB-ieKmoXMhPPExy1fvF_VVdsQ4YwTRFkgZeVx5dNhJU7pddUjz3eFkihO_7vv0Ywhw0YMb07WyM_WvWPAsw2OB9OuM7qotbh42pAOdhNuOOBvbFrMHJwXfUaGsdiSXb4zjZrnXtnn8dGCQYlV0fgR8d7O5__hvvfIxp0FdytjcIOdZnu8OUHfDe3f3FANyhSzUAdwQXa_ppn3PPfJM3TjLw2uEElsJocZ2TcroSeO3-dDnAwIdA_lQZMOe-vzPIiivbTWgGmG7Tvg0wXaqvUDzP4PME0x06n6aLlb0-Hsxh0p69W3YzEejUL0m5n6F-aauCr7YApKHjqTuDWwCC8ez2OBhioxPA4OzIdQDhoVns4iXrugMe3Je_B3lArQMYBZwa9zC0F3Deef0UPgxQfHqYwgsCwus09VPAm3veYTC0e9lDgFcqU0jgtcfOWWhTQHidYQjXK4PKon9MwkCUPmYrA4ragisDAkYjWAz0hJSki7PRPeyBiN0BAUM2sN2BzskSnEaTNHzb6s2KcaBOYcCwwUCtFUhDAoYINzXpy08bGoomT4D6AsOYALbe2w6mv3nkIqieejk2Iy7rt-EUqtOLfKIIGP0lar9AeHhfvV-oLLvxZsADs0T9-2fLTa0uGkJvdqvkQAJvkCxlAg-9rnmcrdILrfDppf38uxrriRK1nvgClO7XYAp1iSe5zt-BF3r0lchk-UHtKcRldsi2LlShA6MWFjQCZ8vXB1_Rqds7J2ZzkYV-KTRslURYaLhMdiZxGdFJjBUGaXhxyBdBA06orvPBc-tAw0vyg4gTbHB-8jlZT_iaQ3xlrDkI-AWgjG6W4sQZh_WTSIX3Cj6nt10tT4lvZAY450Mk7uhSbWxASMMrsmgI01esOAYaTuFLowdY_Uxu2eAF57eIdQkBI4lijHg98GkPdXJnowN9uoyf5zjGboWAd77wPQCec8stAPxAbjMAQ1SIkFS7ueZNPWAVgA_MaUCZAbyRmtVGyG3XmSeF2Dill4gbBrpGiKvVTMgDE5asCrYSCi6P9iS0tRHAEDAiPHD1yNdMFXFxB0tioSFBZJHiTu9xyMdqfI7Y4xAwppdLmcf2OOhRL4N9wsvNmjMFNGAs-dmdwFT9nGhxqGGYoQE9wJxDkwH5LWpTRM4h5VGbohCTVpH9F6yJRNo92or7yd1XfSO4MmLOGcFLF18ZrcWWqbaReikVWoaryI7YHZHFwMxFKaS1nHOs_bDgeiVpbJOEhlW0IaKAiVgrkYaV1NDU-l7N3bD_Eni0kFYpE7FxMuNEF74RclZQxZtFBQ0WUcsowtu1ZQPgLbjjdsAbePfHgAdf8bUKFgJd5hwRJ8aeioD3_2pMHDDGiYd9WwDve-lLkCnkgZS2eKEBvHf4LACuhh-LmAf2_PJd45cCZfHanX0AWO9MFsUlyLyjtwFwLzk4qvqpEoQ1K8pXNuipxVfIYtVTlCvw4h7Vd_bLF0r5xHL8lC58S1rbYTlzoERQVES94r-1HNMVGuxTyzFmsZp3CmzJFHBWjv8CeCwO0wpYd8GQKyL2ZiXm72VEJ5zvSJ6X7ynyq_z_kQJnWI8Kb9JCj_ehfBiBN2r6q_zxmH2Flmyh9o8bCddZJDmNKsNsHZqMKAjLDbzc5tFGc1ZPDw_NXlw3ovBqriRyNScUYKF2acvCXgoqRHWt_vVFxcIxeZj_jyywDuGmw7ZCzFrHIW_d7cRT8AcFUXTnUKsr7zHdXKuY6zvlCjV2lsOCmf1kobIAZy0_peYetb4j-_AOrNaquQd6Mh-ooN4FJvBihRe_vmNNyP7jiQGgpnH-1aAGJgi-0FMDIdE19pvqrrgcw42s68yi0Dgk3cCoRPLg1Sdb3fURMNRZNgURTaASLGo7EGABjH_xsxfOrjfqmFoMEjBRE0XZKztRsmHj8-zHZdap5aHQgHRD_3wSsUUMzbLIBdh8r8gP1WJHaXiNFvf0E0vEppE0oCPdFP56x_oT1AmBh11XDz8Ra8lQAWoMzTzUoXFjyQwttLoMGUqVQ8-yFB4pFd5YlKO2miXmT_jI31znDP3Q8-Y1NxizMRwOOPKAgacKj0rnE5IJq4unPYoptnSEIhDYpB6Mm6FU-O6UL874jN6FYv_khh428KAP8GDFv3gpNe7iX_h7HGHlge1CrX8B");
    	// Vegeta
		registerPreset(2, "DMZ4:CFVmlIVFEUvnfGyqSiVRLCdos2l2wFl5lol6KQCiEwbLPGpZmc5r1A-lVIq2VpmSMWKdNeRBSVORYVQUWrlYX9CPrjr0KI6Ef3vXPefXdOvvKPj3nf991zzv3OOS-Bsf6MexMYYwP6MJc_lHtv-S3xENByClOCxi-6Z09gg5vxYgHZKv77xH8XA-y43G-APTQ6F7DD71QitjgW60l9tBKwTS2DTWzbvtmJiN1GdH9sKABswYRBJja6qfM7YrcT3YeDfIDNH9sCMSx7OQaxPoK9WRUH2OoxAeMXLWfdmiqTZKSNpJJeSSJeERVCSp1imD60H8QrskVsWSzW635zB7ArrJrZ2HKCTagtN8O8L8MUFUfsDhVbFMouL0wB0pCCCXDAgXCz8Uq3XultNttPTpp4xgOkVVAZNduAxAqrrDasEm9gQzniyTy7fQkDmXYpsz_ZsFNAxyD8-oNMxkCPp6pnCxnf1IVQRnHJwD7aNBDY-EqPSg_yNFJ7Sbp5RTOLhUHEkNKJwaTJz5-_AKRg-w0gdYebkZRB4iyaFIEjhcGAXQOW06IRdz0EXDYtBDJdSVkok0lkNk_-TNy4O32xes1CBjF6u7hMlJnlJFNfmQEy4XOfIBe0mFI-O6m5KMNRZv1EL8h0mxclZFoiuyCpgz-fgszacZ0QjfAjysyTMsIT-YYnBoNe3dkKotdsBipksH5KM_MjJKlT0O8hz8tjbx3YuXJs8GontqysZBdPGQK2ln3OjxK2jFyyIyZJuQ57tPBjTiMAx5vmHTnqPRktvMbpSJnu49mJJtvIEs62RqDdkPz4f6v2dM5SkEGj69Y9q9GcIDMgueugmrdf84Ct1XHGa8nZFTO-wNm_zCAEu-PudTiyMmM3sEVBkV1H2LevXiLl-3w_SS2AYMuVwk8SdiscoLA_ZOVRk5wiFyUaE7Cv53-FSxUNhNh6ghUDBLBnwXxaFKZNzG2cJiScBXZbR2H1qRuPNxCSvDkxxRASJhA5dXDZahieunV5oySJh6DxIPpzkdGfcU4z-1rwN9wXvBJtfnnEGVJpfBUypjgE4Z9-Ud2gKn3axh7cHJwMm79lQmnDYtnemZMiyHb1ylZDvwEyClskg2y3TCGeubYUsX7j08w_UY-8f-4wuj-1VtyfSq6w3pTVaq839sTpw8faFmjBmI-OZ06kK7BQNG9-TyolPXdyyOHGjxT7gmDl1qhq8Md-Y9j9wF4RG-AUVT7Q9p7OhpzQwPa88sgNyF4TmeXJA0irWGvY2hq6hdG9cuewd8QP-KlifSYJGRwnSjTW2PPIDcg6pEw846Ws74KKwM6yEvYH");
		// Trunks
		registerPreset(3, "DMZ4:ClVTlOw0AU_U5YQkRBSxeJLmIJJL2VWEIpUiFuEHAIuzAgx2egYCd22CouwAniNCwOXIBD5AiMt_EWz9gaN_6y3nv-7833dx5gBrhaHgByk5CRZP69XEHFiSwUAcyiUx1ZhaTwDcMwnyjVAkAWuDYibaH7PrpnYCxb6vAXxtCWeSXIbCeW6Udk-p6MSJZB7BGB3YpjV9wmHBI5kh2qjCiK9EjaiWWIkewmNkXMZg_LoHHZwOOi8KqqWdgBxnKLgVeOhSzRIct0yEoA4jrxY4sYW0qBXU2BXUuBLftD3DRDnLMPxcYitlO47KZSK9iTqwizWOYydKSYPa92Q-y6wzYH1mFfOexslB16t-6y-x77Oo4d6Vx3Ox94nd_EdZ7I9228b43u-47J9z2T7wcm312m81bjfXfpvjUm3z0m349Mvp-YzvuZyfcL7hwV52aBPvh184OfMDmyfmxzZP3P3cLYk72gm9bytXRNc6Ek3sQWWcdP95Y4NzZQoky1EMkGMn6ZGLYWNINl9DqWyWKZHGRaTZheKFkXiqoR-MH07MHwfHzE_Bp8kE865IsO-aYtdwEvdzBSYIcpsD8psL--SLlDmBLOpNOjA_gH");
		// Gohan
		//registerPreset(4, "DMZ_HAIR:...");
		// Krillin
		registerPreset(5, "DMZHair_v3:7z1cOemO44pBikeI5FSI0DrDGspiel2bhjue82OvVpztIB5YAfJW7zaPvS8cX1waoOVBoNy3SqRTPVqrBGufxb1YHGbVTyPsAX806mL8PAy3ZuDg2lzZ1XVGTIWeujMCCeYRQFl1PWhs9l9KHj66nvVzLswY8w1JlD1EZC9vdx6bqVGh4R9IWMOBm7JRXoSPiGTx0JgWMiiAQEjN9yvNvbHGr4NijAcIB2lUdA13fpdQcpzYy7KzN3SQXG2aFdp7K6YpIJ6dlHrhDW0jJ9GoIlwL6bNww417eWkfA2pptqiYLUDv5BaxjuFgEk27nwOGHxWOlbgNh2oXxz4p7Esh4rckvyxyEBzrU0fVKimRr5lD7yDzKtNuPnqykI7ydWE8nZ2E9X30J4Y991VUnznY7RPaxTS5Q0yW38I0W25OrMlrtbiQHR5Rsyy6qdgM002g7Ep7ulA5JkYARA9X2AJ56Q6rWXWcb6VLYpvEmjrMSgf3vrRhV20AIpLiqmrbpfrwbWdtTBSEhNnKBlCwwm5EEROfRDJgIFNfNB65uQZcuxy5UeTJxIotfOU2cfyzPX1E3MwLVRnBmU56rnfHOkmEbf756nesSq34VXQtlojCpnlYe2TQAoiMHiShDpW4raA01gqq9PsUZ2tecTDXO0jqX7r0Wiyr0hMHbE9uS6TvBFcYcqfnVTxlpkCgu0RXut061gDs1CPUzK77Yu9kjyM9yAVoU7UWUU3ikfHbjRCwBr6Se7iioMPnrEamB9xQygpAZkavEv5S1juBgIVTQeJ6V4eSd68X59UqvnlDgU6p6ZuXusGeKtD8F1bndPWmInUbZ751LGCsVHtdyyrCkfdCqTdeLyv7fnoJRbDn4bej0I3tcgVzZfVvhTzscEPDPGAGSmeEiCpKwuSVabf6eESNqNkL2fq2Be5vfJhzm5RCPcJtH3sDxnYfFWXNwbHcoexJN4rQbmA0e7GnNnvVCtnOXH9KcZnuD508xAVTqbx1x3q7l6Q4mz93LbGgfijUlXHHZJqqagN7j5tOBxEAxqKzLHaX0uQOI8gGD97sCujLrVV5KbmzjDQCq92NskFuTFbEmfiScSYUkQzVYgW7mxzbevtFl6Bg4JbPApp87p3EdFynQwuWF092iVhRKCRIL2seH3V1yKk1YOcKoQ3XnqXQk6Cb83EPbE0wgSteeHR9NBPrWzma0SeyQAahHV3iwiKnMPkh7e5o");
		// Trunks (Large)
		registerPreset(6, "DMZ4:CllktoE1EUhm8eNmkooptqQDBiVBqaNm1DEDFMJwEtWBRERAqCJZoa7UMbLclU1JVSKBKt1TwaW9SFgjtRdFGaSaxaTRUX4kJBceGiq-Je8L5mJrmTyQQmm1xmzvff85_7OOMAoBmYQg4AQMsGYI4nuOc9fjgYT4Q9AKBBkl_Hg7jA9ZfL6InAuwCwAFMMQqfh_zD8N4OadDzJTZdXicxCHZkzDcuIKhlRkYnWl4H0eh16SIv2S0lQqEZJxC8zX6nMWV2ZaDSqUZLCzUArlYk1LKMuCd_59giVOdewKXVt-J_OIJU5XykTSRRdqX2MzLtAK3qV5Hdcz6KBwHPT86ypESpjoTIOW4CReeP_jWVE3405nBZ34vgU1hNTmW9Yr3fx0CuqN1qtJy53_GL08rtCWC_kfXGFpPVocIDSFxl6bfEPQy_snsB0sQ8_gUnAGEqPyzQ8QUfRCbIT6Gr3JKljJpPFCZfkPWZqr1qOmiFe_ZAO_ZDOqpAk5944XBkbEXrzS04CeWTIV1kQCO3f6mCgZ8vbWKiLgU7iiutA3QxEtpwO1EMhK6r4MVTxTWRTEwjK0IEiE3KRky-EW2SZFHMkZNqZSTN0H6XRgaf07arMq2hm7qJEiwp9R4tWZV6UMi8pmc9oZd6Q77vavrP6vmcN-b5nyPd9Q77ThtY7o-07re87a8h3zpDvOUO-84bW-4Eh3_Ny5nAwgQbwwB9AB96Kb-rC4IAb39Qlb7Cf6Eb2PMU3tfQquUT6Bpxp9iFP7m7nvwxx-XLKSmdoovnlHj8hHekCySZR_CH1ALlak10HiY6UcUj5djDVrHUdvQj-_GHKBsyMzFh7ghi24e4KDbd9_0tkYp7NxHD48HZsGLVZkiithcDvbc6TGZRGbpGN24F5KAJsO334BwvcL_cwtDo5sp3cK2uUfK_RdCpCVvRDPuiHfGRal6o3FG693kIguTeAstwbCHQKd-5K6Fq0jYVWmS7EOYP6M31iIFVnrQV9liE7MI2CpvDl-KWxEfAf");
        // Gohan DBS
        registerPreset(7, "DMZ4:CFl3lsVHUQx9-2eFBJFYuEIkKpLYrh6CWKArtvG6ACqdEAmhIUsnZrEWjZcux7RKwHKEkVOXtRyiHUemA4RJMie6AEoaAgUiLiBYmJ8Q-CCZJGgvN-M7_f-73Bxr92s_vm82bmN_Od-aUZRm_DF0wzDOOuW4yUmqi_JTcIXyJRc8qpLOcXy7-tTfxl-5ty0uFLyDa_GvWL84xtduekpxq-SrB-AT7nw2eq4TwSjc8tyxGYQHbgd8Q0NjYhptbwCUyi62oIMT9njidMuY4B6zfH9O_JevHQNrQGR8k6rFtDLO-9X4yxjD7yJGLaH-qLGAwTMBcfuyQw_mdnriZMhdeJYOqZDu-7zaspFfTsi-yV9dnnRQISaX9vQuvJVUfw3a8NXNRDCHFIEvEqGY_8jJr0rJaSui27nF-sOKVYA_vrWhH819wyAs_zHpDZMOic4CWaVxR4rePFpUOEdbLaeVxYv8Tc6uw8iZgDD14gbzpPeDGBjuOD8YDaU5sJM58wKehE4JXwcHTiwOpeiFmCpWXJX2w6MggqdfFGdKtvWQ7xFjC39u2ehG6VrtuGJ9-vYwWCX2--o4cwgy5vYU9hUkKjTiz_GW_EjrkFVKWHCRgMCuJtiWUiplu0EmCieRkiXqf8vP4Fs6u7ef6qGbjr4F70b_KdWxH89rVjun9QHV9M_dzrqFNBxFvEeBu3m4I3oXrOMKzevqKPgffOkIBwS_6lF21Ex4BRrsCA0VNZNXh2Z4p-E0YJN5YaZQQK9LSjQGnezglA8SImLIosZMUpf1YCjxXARy_PYUXhG8GCwr6LRGWKZS2ErFhRyX3IO7X-e0x6iWGgo0rafCOJ5yPeBtGJoai0jsoUW_61It6IFVBJX9P6Azr6waVZCIZ-I_Ao5ujEe3fiaWYZJH8KjH-Bo3-KpgcwPWyb5fiXDh79vxmYM2wp8srPX0FeZORH6OirIhWQ0VmfXSReHuOhNTiat2ozO6oyUdiQAZIpW9WAqfq3tGA5gfO9onRIlp07Nah1LL88KgKH7GTGvGtMpnwFzFH0BmpK9QeMGgSXCP0LWYmZ92wXYNk6tj_0QDumgjTSTsIEoDcU0ht6ec4sEi1Ol-Ny3IA0TO1lcYrAo1_sw71jmYQpYjU1tv9ZVAg1N1VGC_tVsUYeIzwG_66ILIF_S5P7Cfyw3lXTtble29jFiksNEuTp821obRNKj1tT73qPKnF9ZSU2-6F_yr3NlGzBM7OlCtvBNe3LCLPWKxXulqGSvpU6MK5kyrfOa-TmppRESU5H22lkMlrPamF1S4SNBCx5iLt7yhuIWTljBmLgGcJs8C4DJroH1jvQYSu2adwA9LxV9Dpk7cv0-WS9kQ2UC0LAtEqPq0pva18meFJgLZkb258o3M12HledNrGClGEm1M6zavMExOAXwFDb2gm1OvnqGebjD39i2obx6tp2cM-vmLbZDTvYZPE1sMC_HbsH6wYXBqibyvHTEEx6askBb0nXbf9eKUqP9m7BSnJ7vZF5_KkcsrnBgcx1xOgt_smIKIJJGGznnQRuYuC3aFNy1RMTCS2pRE6mViqi3pLNLBV4QpCKLFlKuMmBdWLB83i-tTVzyXqzt_4Sqv627PwRQ7jbrkcjiI6MWtgrjz3yBLNO7i_x1pZqIa3vtjBMbF8_gYmpTODdQJf7jvzJ3jniri--VsWDL0ud3gSRmuSIVC-RkWQdaZNa2CfcuHEdjw6TBVzSLzs49o_ndBXVzJP7KUxuDv7hRcJzbfAxpdgVrmDuwHEwnipRfFiXDCOFiXs4HPbyZN24vITyDx8GnjpNI1XxbjdSKkLGbffnTczPy8-H5E1zktcHy4na5yZvQ5Y5XGytULCknJrUS9FKDhh0jt531Lul0yC7-S4XsYLyVPynp2fzZfVrthXioNCsky-LGaKth8Hk7BhZH_M6ocYWhaBUAkKQFeE2gHGcHSltfK7umnjt0eTWna8rCpYjuO7cMxzcycYBarS2K7izzjjBQqDJFj08XCxdavsC6zZxbYzYxfAXWZ9kzZfZ2MAus_sKi3RF9qTvG1aDtPDJuMF6Nl70rZi6eahNTcpQUG1qxmmW0EpxK9NncYjmg4m7NPBoF7RNpa3GdwwzdXAfNgaDuKtaAXWrpWe0aWWcZdGh51BjafXV-nqne5OX8ThbEYwurbF8C41bi5fULK5aYPwL");
        // Raditz
        registerPreset(8, "DMZ4:hZh7cA1XHMf3JhES6pGitFrxaj2CvKpiqrt7Mx4tw4yiqkNpBNF4NFF2t6XpdNKOmVSDCBERmmqaIghSGuTuJUqINl7xKNUy2ma0RcfQVh_nnt_vnD33sNO_7s6eez-_c36P7-93bqSiRCgeb6SiKK2bKCGZhtbutxjykGJoRdUdyUOGkdz63eOBJVOL-uWPwJJZnf9sh8CSqU4YvySwZLEl67l5U54MVTxphDeNfKaTzzAFwM3SDlKwHmWtAnCzlukAjujdhvJ8XbUfgddkZm7gjRWwgLxUiRfe8yLwSrteoDzv1T5DgBf-c2_AwEOGVe1gpkuYVtdrADO_SylsK3xSNWDazH4VMC3T-9DT-ZJHdUbMDAkT1XczYEYt3wCYDrf7i5gUU6utj6bHtNtWLQYwGrd8d670pBvV9o3YjRZmShYiGidTC3bknXxqgWFMrWnZQvDfBwPaAxicnWEdSFQU5KVJPNyWcPCwLVHAg_OK8WAeUDH0lkZMIXiWW2BWdzoLgbl8pBEDQ5d4YMCjcqBfl3jNZ90F3pL2Z2CjUfFvAS_y803AAw-kWFrV0SfkiKdLvBY0TUiGvzO9F_BC7qViIlIfZ5j-qsLmoiNTLDt0_koK9reZ2B3BsyUwbkLIgXYrTosHJ6H_8G6tiwc04mwEz5HAUA0ErOYUSzUZ9lkyTS41cfjj1ILGYxb6VBm1oGPCWHbqhVtoYa5kwVPaGiykPrZRshAyYxIF6y_Q9CVg5doB2Pq5hm4UrFcuCUPwPLeq37fpklRezCfVXEXwjZhcb0g8PK8REBrgxY9-EXiwrUxL_efYGC5GlOdzXJvhFrPx7T4GXj8qZYSHXjerT4ztCq7l5YW7sFikrUBc0UImt0CUdUxAWR-iptSv6K9TjGoMleH961QVNcWWSCXThwzTW3IzDw5zMPEHMIUOtPxJjZPBlCcm-DCqnVAeHEVvQV02WPApHjgMyxO7_6HRYKEqbhhY6BVaCIepX3EaLfSVLOyOfRgs5BY0gIWb27aAhT30geRJGcUQ8DaqJwTc8e8CAJMlBPeTwLv6XQZ1Y3niHYv6qVaAtJr7MZxmQNTAAi5Zvh7eRyETD7ZMRwv9JQv7qWiQrRcXnwcLjRhp9ICgn2rlVhP0BLugZf_ZvSWCYyXw3u3fA7jw0ssALqe5RMBgU3Q2OtBih7FsUlYIjpPANbveBnD2uHEAXuDfCWDICpKSpF8AuHpHWwpmARaq3RMvgStYs6qA2ja8OfRHBLwjITG4tatbYwwpxT0Jbh6YtLokuJWw5DI1lDnTvsGCBwlD0mNYq_Vg4fq6T9FColveVYJEGMkeOLiTd1xGuI9VLFzLLlwcj-CnJTCWl8Eqz2AeNZlHTR_2CdO_KDsNLGAuWXYNTVtSlKTFoYUBkgX0gOAcNhkwvzkK4w8FRXCcw4wLmugZKFn4Jmm7lNm_oibiEvE6UWgA1z7zPOTJA5yTJErXWGcoVDdjy9SjafAyDJ2ECiy8h51SzQKFcTJbdxIwV9pxCbQmg23C8J5vOgB4n8gNzV9EfUws4K8smyWM96OyhWhhmWRhA2torBczWTLVLCpdhLe0sxZcMj4-fXmWu-SJI3xJdFbg4i1kBRssLDsrcyryVki8w5VbgZf3ygTgxUy7DTxYIkLKxkQmBI7MeUmeIDhPAtd_OcINjHliYt8m4ON7HoFKYR7QnR2vlMB1AxcBOJ2GXvTA0S-ugH5yNeI73lzTCcDkMAjOdyuQnLMvAbgDHRy5fvICEZoVq1dh0POsctsx7yncFXhwk-m7M-qyX1nslmPpjvCtdqu8gRFFYCEbJIJlxYOiqLFhX1Dogv8Fc4WG_ZGSZvMY3zGfd4SSXuMGtun4T8CjcJjmYKcL3ucKZ0b1FErgHeVDg68T3tfoPS_QU-gS4eF3hB7lZMVat-DdooEhvGFzD4nBE_bnY11a-33qROQVubX_2PfXAi-f3mm4DBMe6rFTDoIgrJN4_p3DpebMQw-BFiLuNCIn4sWcRx4WkIcQorlDA5obBgZQqA29cd81KVKgEOmZas6xOikXztMvE41EpbYCo4e09XUbvw2eodlob6rLojNBItgg6t--4B6Q82hchY6E5MDM60ELIbJz-CCaQLWbWCjFuyq_BLPpy9L5JKSESDvGUwnqyych6NLiiAUKGDSTh3JeMyVkRorStFvskLjYuDji8pEBl7cIbkvaTxd7gJn1uYPADMwUpDhIhkkjIrvAOf1OOexWdeXU9QS8iCXLKeoG4XKhnhx0NThrhLvsEQkMyivMReziwESZhBKuxASMbyy7S9YaSdmUWgnMLiv8Ep_cnJUftkJTW0rrkORINBvp8AJi2RgnS_-u42C0cNRtusXbgTDx8-sQU2cvt4BLll5C4yCWvHJMsoCONPTDN6ZIHQuUg_CwDzvjkHANr3OTpPX3TUGsZwcKPHijNv49JYKPu22UaYkz3bL00Hl6o0yL5fe1xGvYWwE8vNEZ3n8huUz13OCR0rQMXw5y5AmJx6WIl_NFejfmB08xWU6Zej3852GhCJAc4N1IOek2VfH0raERF244zMfMo0EbPSPxoDOIyYpvTJ0PwPhGjEeDIA-eOUp48puZ8-fOVv4D");

    }

    private static final String BASE64_URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final int BASE = 64;

    private static byte[] compressOptimized(byte[] data) throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        DeflaterOutputStream defOut = new DeflaterOutputStream(byteOut, deflater);
        defOut.write(data);
        defOut.close();
        deflater.end();
        return byteOut.toByteArray();
    }

    private static byte[] decompressOptimized(byte[] data) throws Exception {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        Inflater inflater = new Inflater(true);
        InflaterInputStream infIn = new InflaterInputStream(byteIn, inflater);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = infIn.read(buffer)) != -1) {
            byteOut.write(buffer, 0, len);
        }
        infIn.close();
        inflater.end();
        return byteOut.toByteArray();
    }

    private static String encodeToNumbers(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        BigInteger value = new BigInteger(1, bytes);

        if (value.equals(BigInteger.ZERO)) {
            return String.valueOf(BASE64_URL_ALPHABET.charAt(0));
        }

        StringBuilder result = new StringBuilder();
        BigInteger base = BigInteger.valueOf(BASE);

        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = value.divideAndRemainder(base);
            result.append(BASE64_URL_ALPHABET.charAt(divmod[1].intValue()));
            value = divmod[0];
        }

        return result.reverse().toString();
    }

    private static byte[] decodeFromNumbers(String encoded) {
        return decodeBase64Url(encoded);
    }

    private static byte[] decodeBase64Url(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new byte[0];

        BigInteger value = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(64);

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int digit = BASE64_URL_ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid character in Base64URL string: " + c);
            }
            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }

        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] result = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, result, 0, result.length);
            return result;
        }
        return bytes;
    }

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static byte[] decodeBase62(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new byte[0];

        BigInteger value = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(62);

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int digit = BASE62_ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
            }
            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }

        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] result = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, result, 0, result.length);
            return result;
        }

        return bytes;
    }

    public static String toCode(CustomHair hair) {
		if (hair == null) return "";
		try {
			CompoundTag tag = hair.save();
			ByteArrayOutputStream nbtOut = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(nbtOut);
			NbtIo.write(tag, dataOut);
			dataOut.close();
			byte[] nbtBytes = nbtOut.toByteArray();

			byte[] compressed = compressOptimized(nbtBytes);

			return CODE_PREFIX_V4 + encodeToNumbers(compressed);
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Failed to serialize CustomHair to code", e);
			return "";
		}
	}

	public static CustomHair fromCode(String code) {
		if (code == null || code.isEmpty()) return null;

		if (isFullSetCode(code)) {
			CustomHair[] set = fromFullSetCode(code);
			return (set != null && set.length > 0) ? set[0] : null;
		}

		try {
			String cleanCode = code;
			boolean isV4 = code.startsWith(CODE_PREFIX_V4);
			boolean isLegacy = false;

			if (isV4) {
				cleanCode = code.substring(CODE_PREFIX_V4.length());
			} else if (code.startsWith(CODE_PREFIX_V3)) {
				cleanCode = code.substring(CODE_PREFIX_V3.length());
				isLegacy = true;
			} else if (code.startsWith(CODE_PREFIX_V2)) {
				cleanCode = code.substring(CODE_PREFIX_V2.length());
				isLegacy = true;
			} else if (code.startsWith(CODE_PREFIX_V1)) {
				cleanCode = code.substring(CODE_PREFIX_V1.length());
				isLegacy = true;
			}

			byte[] bytes = isLegacy ? decodeBase62(cleanCode) : decodeFromNumbers(cleanCode);
			CompoundTag tag;

			if (isV4) {
				byte[] decompressed = decompressOptimized(bytes);
				ByteArrayInputStream byteIn = new ByteArrayInputStream(decompressed);
				DataInputStream dataIn = new DataInputStream(byteIn);
				tag = NbtIo.read(dataIn);
			} else {
				ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
				GZIPInputStream gzipIn = new GZIPInputStream(byteIn);
				DataInputStream dataIn = new DataInputStream(gzipIn);
				tag = NbtIo.read(dataIn);
			}

			if (tag.contains("Base") && (tag.contains("SSJ") || tag.contains("SSJ2") || tag.contains("SSJ3"))) {
				CustomHair base = new CustomHair();
				if (tag.contains("Base")) base.load(tag.getCompound("Base"));
				return base;
			}

			CustomHair hair = new CustomHair();
			hair.load(tag);
			return hair;
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Failed to deserialize CustomHair from code", e);
			return null;
		}
	}

	public static String toFullSetCode(CustomHair base, CustomHair ssj, CustomHair ssj3) {
		if (base == null) base = new CustomHair();
		if (ssj == null) ssj = base.copy();
		if (ssj3 == null) ssj3 = base.copy();

		try {
			CompoundTag fullSetTag = new CompoundTag();
			fullSetTag.put("B", base.save());
			fullSetTag.put("S", ssj.save());
			fullSetTag.put("T", ssj3.save());

			ByteArrayOutputStream nbtOut = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(nbtOut);
			NbtIo.write(fullSetTag, dataOut);
			dataOut.close();
			byte[] nbtBytes = nbtOut.toByteArray();

			byte[] compressed = compressOptimized(nbtBytes);

			return CODE_PREFIX_FULL_V4 + encodeToNumbers(compressed);
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Failed to serialize CustomHair full set to code", e);
			return "";
		}
	}

	public static CustomHair[] fromFullSetCode(String code) {
		if (code == null) return null;

		boolean isV4 = code.startsWith(CODE_PREFIX_FULL_V4);
		boolean isV3 = code.startsWith(CODE_PREFIX_FULL);

		if (!isV4 && !isV3) return null;

		try {
			String cleanCode = isV4 ?
				code.substring(CODE_PREFIX_FULL_V4.length()) :
				code.substring(CODE_PREFIX_FULL.length());

			byte[] bytes = isV4 ? decodeFromNumbers(cleanCode) : decodeBase62(cleanCode);
			CompoundTag fullSetTag;

			if (isV4) {
				byte[] decompressed = decompressOptimized(bytes);
				ByteArrayInputStream byteIn = new ByteArrayInputStream(decompressed);
				DataInputStream dataIn = new DataInputStream(byteIn);
				fullSetTag = NbtIo.read(dataIn);
			} else {
				ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
				GZIPInputStream gzipIn = new GZIPInputStream(byteIn);
				DataInputStream dataIn = new DataInputStream(gzipIn);
				fullSetTag = NbtIo.read(dataIn);
			}

			CustomHair base = null, ssj = null, ssj3 = null;

			if (fullSetTag.contains("B") || fullSetTag.contains("Base")) {
				base = new CustomHair();
				base.load(fullSetTag.contains("B") ? fullSetTag.getCompound("B") : fullSetTag.getCompound("Base"));
			}
			if (fullSetTag.contains("S") || fullSetTag.contains("SSJ")) {
				ssj = new CustomHair();
				ssj.load(fullSetTag.contains("S") ? fullSetTag.getCompound("S") : fullSetTag.getCompound("SSJ"));
			}
			if (fullSetTag.contains("T") || fullSetTag.contains("SSJ3")) {
				ssj3 = new CustomHair();
				ssj3.load(fullSetTag.contains("T") ? fullSetTag.getCompound("T") : fullSetTag.getCompound("SSJ3"));
			}

			if (base == null || ssj == null || ssj3 == null) return null;

			return new CustomHair[]{base, ssj, ssj3};

		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Failed to deserialize full set code, trying fallback format", e);
			return fromFullSetCodeFallback(code);
		}
	}

	private static CustomHair[] fromFullSetCodeFallback(String code) {
		if (code == null || !code.startsWith(CODE_PREFIX_FULL)) return null;

		String rawData = code.substring(CODE_PREFIX_FULL.length());
		String[] parts = rawData.split(FULL_SET_SEPARATOR);

		if (parts.length < 3) return null;

		CustomHair base = fromCode(parts[0]);
		CustomHair ssj = fromCode(parts[1]);
		CustomHair ssj3 = fromCode(parts[2]);

		if (base == null || ssj == null || ssj3 == null) return null;

		return new CustomHair[]{base, ssj, ssj3};
	}

	public static boolean isFullSetCode(String code) {
		return code != null && (code.startsWith(CODE_PREFIX_FULL) || code.startsWith(CODE_PREFIX_FULL_V4));
	}

    public static boolean canUseHair(Character character) {
        if (character == null) return false;
        String race = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();

        for (String defaultRace : DEFAULT_HAIR_RACES) if (race.equals(defaultRace)) return true;

        if (race.equals("majin")) return gender.equals(Character.GENDER_FEMALE);
        if (race.equals("bioandroid") || race.equals("frostdemon") || race.equals("namekian")) return false;

        RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
        return config != null && config.canUseHair();
    }

    public static CustomHair getEffectiveHair(Character character) {
        if (!canUseHair(character)) return null;
        int hairId = character.getHairId();
        if (hairId == 0) {
            CustomHair custom = character.getHairBase();
            if (custom == null) {
                custom = new CustomHair();
                character.setHairBase(custom);
            }
            return custom;
        }

        return getPresetHair(hairId, character.getHairColor());
    }

    public static CustomHair getPresetHair(int presetId, String hairColor) {
        String code = PRESET_CODES.get(presetId);
		if (code == null) return new CustomHair();

		if (!PRESET_CACHE.containsKey(presetId)) {
			CustomHair baseHair = fromCode(code);
			if (baseHair != null) {
				PRESET_CACHE.put(presetId, baseHair);
			} else {
				return new CustomHair();
			}
		}

		CustomHair hair = PRESET_CACHE.get(presetId).copy();
		if (hair != null) {
			if (hairColor != null && !hairColor.isEmpty()) {
				hair.setGlobalColor(hairColor);
			}
			return hair;
		}

		CustomHair basic = new CustomHair();
		if (hairColor != null && !hairColor.isEmpty()) {
			basic.setGlobalColor(hairColor);
		}
		return basic;
	}

    public static void registerPreset(int presetId, String code) {
        if (presetId > 0 && code != null && !code.isEmpty()) {
            PRESET_CODES.put(presetId, code);
        }
    }

    public static int getPresetCount() {
        return PRESET_CODES.size();
    }
}
