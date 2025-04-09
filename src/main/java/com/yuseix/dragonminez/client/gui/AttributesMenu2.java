package com.yuseix.dragonminez.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuseix.dragonminez.client.RenderEntityInv;
import com.yuseix.dragonminez.client.gui.buttons.CustomButtons;
import com.yuseix.dragonminez.client.gui.buttons.DMZGuiButtons;
import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.events.characters.StatsEvents;
import com.yuseix.dragonminez.common.network.C2S.StatsC2S;
import com.yuseix.dragonminez.common.network.C2S.ZPointsC2S;
import com.yuseix.dragonminez.common.network.ModMessages;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.client.config.DMZClientConfig;
import com.yuseix.dragonminez.common.util.DMZDatos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class AttributesMenu2 extends Screen implements RenderEntityInv {

    private int alturaTexto; private int anchoTexto; private int multiplicadorTP = 1;

    private static final ResourceLocation menucentro = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/menulargo2.png");


    private CustomButtons multiBoton, strBoton, defBoton, conBoton, pwrBoton, eneBoton; private DMZDatos dmzdatos = new DMZDatos();
    private DMZGuiButtons newMenuBoton;

    // Formateador de números con separadores (por ejemplo, "10.000.000")
    NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);

    public AttributesMenu2() {
        super(Component.empty());
    }


    @Override
    public void init() {
        super.init();
    }

    @Override
    public void tick() {
        super.tick();
        botonesStats();
        botonesMenus();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(graphics);
        //Paneles del menu
        menuPaneles(graphics);
        menu1info(graphics, pMouseX, pMouseY);
        menu2info(graphics, pMouseX, pMouseY);
        menu0info(graphics, pMouseX, pMouseY);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
    }

    public void botonesMenus(){
        alturaTexto = (this.height + 168)/2;
        anchoTexto = (this.width)/2;

        if (this.minecraft.level.isClientSide) {
            Player player = this.minecraft.player;
            this.newMenuBoton = this.addRenderableWidget(new DMZGuiButtons(anchoTexto - 85, alturaTexto, "stats", Component.empty(), wa -> {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
                    if (!playerstats.getBoolean("compactmenu")) {
                        this.minecraft.setScreen(new AttributesMenu());
                    }});
            }));
            this.newMenuBoton = this.addRenderableWidget(new DMZGuiButtons(anchoTexto - 55, alturaTexto, "skills", Component.empty(), wa -> {
                this.minecraft.setScreen(new SkillMenu(false));
            }));
            this.newMenuBoton = this.addRenderableWidget(new DMZGuiButtons(anchoTexto - 25, alturaTexto, "transf", Component.empty(), wa -> {
                this.minecraft.setScreen(new TransfMenu(false));
            }));
            this.newMenuBoton = this.addRenderableWidget(new DMZGuiButtons(anchoTexto + 5, alturaTexto, "storyline", Component.empty(), wa -> {
                this.minecraft.setScreen(new StorylineMenu(false));
            }));
            this.newMenuBoton = this.addRenderableWidget(new DMZGuiButtons(anchoTexto + 35, alturaTexto, "kitech", Component.empty(), wa -> {
                // Agregar acá el menú de Ki Techniques
                // this.minecraft.setScreen(new TransfMenu());
            }));
            this.newMenuBoton = this.addRenderableWidget(new DMZGuiButtons(anchoTexto + 65, alturaTexto, "settings", Component.empty(), wa -> {
                this.minecraft.setScreen(new ConfigMenu());
            }));
        }
    }

    public void botonesStats(){
        this.removeWidget(strBoton);
        this.removeWidget(defBoton);
        this.removeWidget(conBoton);
        this.removeWidget(pwrBoton);
        this.removeWidget(eneBoton);

        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, Minecraft.getInstance().player).ifPresent(playerstats -> {
            var tps = playerstats.getIntValue("tps"); var str = playerstats.getStat("STR"); var def = playerstats.getStat("DEF");
            var con = playerstats.getStat("CON"); var kipower = playerstats.getStat("PWR"); var energy = playerstats.getStat("ENE");

            anchoTexto = (this.width/2)-110;alturaTexto = (this.height / 2) -15;

            int maxStats = DMZClientConfig.getMaxStats();
            int nivel = (str + def + con + kipower + energy) / 5;
            int baseCost = (int) Math.round((nivel * DMZClientConfig.getMultiplierZPoints())
                    * DMZClientConfig.getMultiplierZPoints() * 1.5);

            this.multiBoton = (CustomButtons) this.addRenderableWidget(new CustomButtons("stat", anchoTexto - 3, alturaTexto + 63, Component.empty(), wa -> {
                switch (multiplicadorTP) {
                    case 1 -> multiplicadorTP = 10;
                    case 10 -> multiplicadorTP = 100;
                    case 100 -> multiplicadorTP = 1;
                }
            }));

            // Calcula el número de niveles a aumentar de forma uniforme
            int upgradeStatSTR = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, str, tps, baseCost, maxStats);
            int upgradeStatDEF = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, def, tps, baseCost, maxStats);
            int upgradeStatCON = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, con, tps, baseCost, maxStats);
            int upgradeStatPWR = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, kipower, tps, baseCost, maxStats);
            int upgradeStatENE = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, energy, tps, baseCost, maxStats);

            // Calcula el costo ajustado para cada stat
            int finalCostSTR = (upgradeStatSTR > 0) ? StatsEvents.calcularCostoRecursivo(nivel, upgradeStatSTR, baseCost, maxStats): Integer.MAX_VALUE;
            int finalCostDEF = (upgradeStatDEF > 0) ? StatsEvents.calcularCostoRecursivo(nivel, upgradeStatDEF, baseCost, maxStats): Integer.MAX_VALUE;
            int finalCostCON = (upgradeStatCON > 0) ? StatsEvents.calcularCostoRecursivo(nivel, upgradeStatCON, baseCost, maxStats): Integer.MAX_VALUE;
            int finalCostPWR = (upgradeStatPWR > 0) ? StatsEvents.calcularCostoRecursivo(nivel, upgradeStatPWR, baseCost, maxStats): Integer.MAX_VALUE;
            int finalCostENE = (upgradeStatENE > 0) ? StatsEvents.calcularCostoRecursivo(nivel, upgradeStatENE, baseCost, maxStats): Integer.MAX_VALUE;

            // Crear botones solo si hay suficiente ZPoints y si es posible aumentar el stat
            if (tps >= finalCostSTR && str < maxStats) {
                this.strBoton = (CustomButtons) this.addRenderableWidget(new CustomButtons("stat",anchoTexto, alturaTexto,Component.empty(), wa -> {
                    // Se comprueba nuevamente con cada click del botón
                    int actualTps = playerstats.getIntValue("tps");
                    int actualStr = playerstats.getStat("STR");
                    int actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, actualStr, actualTps, baseCost, maxStats);
                    int actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);
                    if (actualTps >= actualCost && actualStr < maxStats) {
                        ModMessages.sendToServer(new ZPointsC2S(1, actualCost));
                        ModMessages.sendToServer(new StatsC2S(0, actualUpgrade));
                    }
                }));}
            if (tps >= finalCostDEF && def < maxStats) {
                this.defBoton = (CustomButtons) this.addRenderableWidget(new CustomButtons("stat",anchoTexto, alturaTexto + 12,Component.empty(), wa -> {
                    int actualTps = playerstats.getIntValue("tps");
                    int actualDef = playerstats.getStat("DEF");
                    int actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, actualDef, actualTps, baseCost, maxStats);
                    int actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);
                    if (actualTps >= actualCost && actualDef < maxStats) {
                        ModMessages.sendToServer(new ZPointsC2S(1, actualCost));
                        ModMessages.sendToServer(new StatsC2S(1, actualUpgrade));
                    }
                }));}
            if (tps >= finalCostCON && con < maxStats) {
                this.conBoton = (CustomButtons) this.addRenderableWidget(new CustomButtons("stat",anchoTexto, alturaTexto + 24,Component.empty(), wa -> {
                    int actualTps = playerstats.getIntValue("tps");
                    int actualCon = playerstats.getStat("CON");
                    int actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, actualCon, actualTps, baseCost, maxStats);
                    int actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);
                    if (actualTps >= actualCost && actualCon < maxStats) {
                        ModMessages.sendToServer(new ZPointsC2S(1, actualCost));
                        ModMessages.sendToServer(new StatsC2S(2, actualUpgrade));
                    }
                }));}
            if (tps >= finalCostPWR && kipower < maxStats) {
                this.pwrBoton = (CustomButtons) this.addRenderableWidget(new CustomButtons("stat",anchoTexto, alturaTexto + 36,Component.empty(), wa -> {
                    int actualTps = playerstats.getIntValue("tps");
                    int actualPwr = playerstats.getStat("PWR");
                    int actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, actualPwr, actualTps, baseCost, maxStats);
                    int actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);
                    if (actualTps >= actualCost && actualPwr < maxStats) {
                        ModMessages.sendToServer(new ZPointsC2S(1, actualCost));
                        ModMessages.sendToServer(new StatsC2S(3, actualUpgrade));
                    }
                }));}
            if (tps >= finalCostENE && energy < maxStats) {
                this.eneBoton = (CustomButtons) this.addRenderableWidget(new CustomButtons("stat",anchoTexto, alturaTexto + 48,Component.empty(), wa -> {
                    int actualTps = playerstats.getIntValue("tps");
                    int actualEne = playerstats.getStat("ENE");
                    int actualUpgrade = StatsEvents.calcularNivelesAumentar(nivel, multiplicadorTP, actualEne, actualTps, baseCost, maxStats);
                    int actualCost = StatsEvents.calcularCostoRecursivo(nivel, actualUpgrade, baseCost, maxStats);
                    if (actualTps >= actualCost && actualEne < maxStats) {
                        ModMessages.sendToServer(new ZPointsC2S(1, actualCost));
                        ModMessages.sendToServer(new StatsC2S(4, actualUpgrade));
                    }
                }));
            }
        });
    }

    public void menu0info(GuiGraphics guiGraphics, int mouseX, int mouseY){
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, Minecraft.getInstance().player).ifPresent(playerstats -> {

            alturaTexto = (this.height / 2) - 65; anchoTexto = (this.width/2)+50;

            var playername = Minecraft.getInstance().player.getName().getString(); var alignment = playerstats.getIntValue("alignment");
            var raza = playerstats.getIntValue("race"); int namecolor;
            if (alignment > 60) {
                namecolor = 0x63FFFF;
            } else if (alignment > 40) {
                namecolor = 0xeaa8fe;
            } else {
                namecolor = 0xFA5252;
            }
            drawStringWithBorder(guiGraphics, font, Component.literal(playername).withStyle(ChatFormatting.BOLD), anchoTexto, alturaTexto, namecolor);

            if (mouseX >= anchoTexto - 10 && mouseX <= anchoTexto + 10 && mouseY >= alturaTexto && mouseY <= alturaTexto + font.lineHeight) {
                List<FormattedCharSequence> descriptionLines = new ArrayList<>();
                if (alignment > 60) {
                    descriptionLines.add(Component.translatable("stats.dmz.alignment_good", alignment).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                } else if (alignment > 40) {
                    descriptionLines.add(Component.translatable("stats.dmz.alignment_neutral", alignment).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                } else {
                    descriptionLines.add(Component.translatable("stats.dmz.alignment_evil", alignment).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                }
                guiGraphics.renderTooltip(font, descriptionLines, mouseX, mouseY);
            }

            alturaTexto = (this.height / 2) - 54; anchoTexto = (this.width/2)+50;
            String[] razasString = {"human", "saiyan", "namek", "bioandroid", "colddemon", "majin"};
            int[] razasInt = {0, 1, 2, 3, 4, 5};
            int[] colors = {0x177CFC, 0xFCB317, 0x186814, 0x7DFF76, 0x6A31EE, 0xFF86FD};
            for (int i = 0; i < razasString.length; i++) {
                String razaActual = razasString[i];
                int razas = razasInt[i];
                int color = colors[i];
                if (raza == razas) {
                    drawStringWithBorder(guiGraphics, font, Component.translatable("dmz.races.name." + razaActual), anchoTexto+2, alturaTexto, color);
                }
            }
        });
    }

    public void menu1info(GuiGraphics graphics, int mouseX, int mouseY){
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, Minecraft.getInstance().player).ifPresent(playerstats -> {
            var TPS = playerstats.getIntValue("tps");
            var nivel = (playerstats.getStat("STR") + playerstats.getStat("DEF") + playerstats.getStat("CON")
                    + playerstats.getStat("PWR") + playerstats.getStat("ENE")) / 5;
            var clase = playerstats.getStringValue("class");

            //VARIABLES:
            //NIVEL TPS
            anchoTexto = (this.width/2)-72; alturaTexto = (this.height / 2) - 64;
            drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(nivel)), anchoTexto, alturaTexto, 0xFFFFFF);
            drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(TPS)), anchoTexto, alturaTexto + 11, 0xFFE593);

            //FORMA
            drawStringWithBorder2(graphics, font, Component.translatable(obtenerFormaLang(playerstats.getStringValue("form"), playerstats.getIntValue("race"), playerstats.getStringValue("gender"))), anchoTexto+4, alturaTexto + 22, 0xC7EAFC);
            //Clase
            if(clase.equals("warrior")){
                drawStringWithBorder2(graphics, font,Component.translatable("gui.dmz.stats.warrior"), anchoTexto, alturaTexto + 33, 0xFC4E2B);
            }else {
                drawStringWithBorder2(graphics, font,Component.translatable("gui.dmz.stats.spiritualist"), anchoTexto, alturaTexto + 33, 0x2BFCFC);
            }

            var strdefault = playerstats.getStat("STR"); var defdefault = playerstats.getStat("DEF"); var condefault = playerstats.getStat("CON");
            var kipowerdefault = playerstats.getStat("PWR"); var energydefault = playerstats.getStat("ENE"); var raza = playerstats.getIntValue("race");
            var transf = playerstats.getStringValue("form");

            //Efectos
            var majinOn = playerstats.hasDMZPermaEffect("majin"); var frutaOn = playerstats.hasDMZTemporalEffect("mightfruit");

            var baseCost =  (int) Math.round((((nivel * DMZClientConfig.getMultiplierZPoints())) * DMZClientConfig.getMultiplierZPoints() ) * 1.5);
            int costoRecursivo = StatsEvents.calcularCostoRecursivo(nivel, multiplicadorTP, baseCost, DMZClientConfig.getMaxStats());

            var strcompleta = dmzdatos.calcMultipliedStrength(playerstats);
            var defcompleta = dmzdatos.calcMultipliedDefense(playerstats);
            var pwrcompleta = dmzdatos.calcMultipliedKiPower(playerstats);

            var STRMulti = Math.round((dmzdatos.calcStatMultiplier(playerstats, "STR")) * 100) / 100.0;
            var DEFMulti = Math.round((dmzdatos.calcStatMultiplier(playerstats, "DEF")) * 100) / 100.0;
            var KIPOWERMulti = Math.round((dmzdatos.calcStatMultiplier(playerstats, "PWR")) * 100) / 100.0;
            var multiTotal = dmzdatos.calcTotalMultiplier(playerstats);

            var isMultiOn = majinOn || frutaOn || !Objects.equals(transf, "base") || playerstats.getIntValue("race") == 4;
            var colorEnForma = isMultiOn ? 0xfebc0d : 0xFFD7AB;


            //WA
            Component STRReal = Component.empty()
                    .append(Component.literal(numberFormatter.format(strcompleta)))
                    .append(Component.literal(" x")
                            .append(Component.literal(numberFormatter.format(STRMulti)))
                    );
            Component DEFReal = Component.empty()
                    .append(Component.literal(numberFormatter.format(defcompleta)))
                    .append(Component.literal(" x")
                            .append(Component.literal(numberFormatter.format(DEFMulti)))
                    );
            Component PWRReal = Component.empty()
                    .append(Component.literal(numberFormatter.format(pwrcompleta)))
                    .append(Component.literal(" x")
                            .append(Component.literal(numberFormatter.format(KIPOWERMulti)))
                    );

            //Titulos
            anchoTexto = (this.width / 2) - 110; alturaTexto = (this.height / 2) - 64;

            graphics.drawString(font, Component.translatable("gui.dmz.stats.form").withStyle(ChatFormatting.BOLD),anchoTexto, alturaTexto + 22, 0xD7FEF5);
            graphics.drawString(font, Component.translatable("gui.dmz.stats.class").withStyle(ChatFormatting.BOLD),anchoTexto, alturaTexto + 33, 0xD7FEF5);

            String[] stats = { "Level", "TPs", "STR", "DEF", "CON", "PWR", "ENE", "TPC"};
            int[] colors = { 0xD7FEF5, 0xD7FEF5, 0xD71432, 0xD71432, 0xD71432, 0xD71432, 0xD71432, 0x2BFFE2};
            for (int i = 0; i < stats.length; i++) {
                String statKey = stats[i];
                int colores = colors[i];
                int yOffset;
                if (statKey.equals("Level") || statKey.equals("TPs")) {
                    yOffset = alturaTexto + (i * 11); // Valor fijo para "Level" y "TPs"
                } else if (statKey.equals("TPC")) {
                    alturaTexto = (this.height / 2) -14;
                    yOffset = (alturaTexto) + ((i-2) * 12) + 4; // Valor fijo para "TPC"
                } else {
                    alturaTexto = (this.height / 2) -14;
                    yOffset = (alturaTexto) + ((i-2) * 12); // Valor general para otras stats
                }
                if (statKey.equals("Level") || statKey.equals("TPs")) {
                    anchoTexto = (this.width / 2) - 110;
                } else if (statKey.equals("TPC")) {
                    anchoTexto = (this.width/2)-99;
                } else {
                    anchoTexto = (this.width/2)-95;
                }

                String statLang = statKey.toLowerCase(Locale.ROOT);
                Component statComponent = Component.translatable("gui.dmz.stats." + statLang)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colores)).withBold(true));
                graphics.drawString(font, statComponent, anchoTexto, yOffset, colores);

                if (mouseX >= anchoTexto - 10 && mouseX <= anchoTexto + 25 && mouseY >= yOffset && mouseY <= yOffset + font.lineHeight) {
                    List<FormattedCharSequence> descriptionLines = new ArrayList<>();
                    FormattedText descriptionText = Component.translatable("stats.dmz." + statKey.toLowerCase());
                    List<FormattedCharSequence> lines = font.split(descriptionText, 250);
                    descriptionLines.addAll(lines);

                    FormattedText descText = Component.translatable("stats.dmz." + statKey.toLowerCase() + ".desc");
                    List<FormattedCharSequence> descLines = font.split(descText, 250);
                    descriptionLines.addAll(descLines);

                    if (statKey.equals("STR") && multiTotal > 1 || statKey.equals("STR") && playerstats.getIntValue("race") == 4) {
                        descriptionLines.add(Component.translatable("stats.dmz.original", numberFormatter.format(strdefault)).withStyle(ChatFormatting.RED).getVisualOrderText());
                        descriptionLines.add(Component.translatable("stats.dmz.modified", numberFormatter.format(strcompleta)).withStyle(ChatFormatting.GOLD).getVisualOrderText());
                    } else if (statKey.equals("DEF") && multiTotal > 1 || statKey.equals("DEF") && playerstats.getIntValue("race") == 4) {
                        descriptionLines.add(Component.translatable("stats.dmz.original", numberFormatter.format(defdefault)).withStyle(ChatFormatting.RED).getVisualOrderText());
                        descriptionLines.add(Component.translatable("stats.dmz.modified", numberFormatter.format(defcompleta)).withStyle(ChatFormatting.GOLD).getVisualOrderText());
                    } else if (statKey.equals("PWR") && multiTotal > 1 || statKey.equals("PWR") && playerstats.getIntValue("race") == 4) {
                        descriptionLines.add(Component.translatable("stats.dmz.original", numberFormatter.format(kipowerdefault)).withStyle(ChatFormatting.RED).getVisualOrderText());
                        descriptionLines.add(Component.translatable("stats.dmz.modified", numberFormatter.format(pwrcompleta)).withStyle(ChatFormatting.GOLD).getVisualOrderText());
                    }
                    graphics.renderTooltip(font, descriptionLines, mouseX, mouseY);
                }
            }
            //STATS CAPABILITY
            alturaTexto = (this.height / 2) -14; anchoTexto = (this.width/2)-65;

            if(isMultiOn || playerstats.getIntValue("race") == 4){ //Si alguna forma, estado esta activo.
                drawStringWithBorder2(graphics, font, STRReal, anchoTexto, alturaTexto, colorEnForma);
                drawStringWithBorder2(graphics, font, DEFReal, anchoTexto, alturaTexto + 12, colorEnForma);
                drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(condefault)), anchoTexto, alturaTexto + 24, 0xFFD7AB);
                drawStringWithBorder2(graphics, font, PWRReal, anchoTexto, alturaTexto + 36, colorEnForma);
                drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(energydefault)), anchoTexto, alturaTexto + 48, 0xFFD7AB);
            } else {
                drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(strdefault)), anchoTexto, alturaTexto, 0xFFD7AB);
                drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(defdefault)), anchoTexto, alturaTexto + 12, 0xFFD7AB);
                drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(condefault)), anchoTexto, alturaTexto + 24, 0xFFD7AB);
                drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(kipowerdefault)), anchoTexto, alturaTexto + 36, 0xFFD7AB);
                drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(energydefault)), anchoTexto, alturaTexto + 48, 0xFFD7AB);
            }

            anchoTexto = (this.width/2)-65;
            drawStringWithBorder2(graphics, font, Component.literal(numberFormatter.format(costoRecursivo)), anchoTexto, alturaTexto + 64, 0xFFCE41);
            drawStringWithBorder2(graphics, font, Component.literal("x" + multiplicadorTP), anchoTexto, alturaTexto + 76, 0x2BFFE2);

        });

    }

    public void menu2info(GuiGraphics graphics, int mouseX, int mouseY){
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, Minecraft.getInstance().player).ifPresent(playerstats -> {

            anchoTexto = (this.width/2+2); alturaTexto = (this.height / 2) -37;

            //Information title
            drawStringWithBorder2(graphics, font, Component.translatable("gui.dmz.stats.statistics"), anchoTexto, alturaTexto, 0xF91E64);

            //Titulos
            anchoTexto = (this.width/2+2); alturaTexto = (this.height / 2) -25;

            var color = 0xFBA16A;
            String[] stats = {"Damage", "Defense", "Health", "Stamina", "Ki Damage", "Max Ki"};
            int[] colors = {0xFBA16A, 0xFBA16A, 0xFBA16A, 0xFFBB91, 0xFBA16A, 0xFBA16A};
            for (int i = 0; i < stats.length; i++) {
                String statKey = stats[i];
                int colores = colors[i];
                int yOffset = alturaTexto + (i * 12);
                String statLang = "";
                switch (statKey) {
                    case "Damage" -> statLang = "gui.dmz.stats.damage";
                    case "Defense" -> statLang = "gui.dmz.stats.defense";
                    case "Health" -> statLang = "gui.dmz.stats.health";
                    case "Stamina" -> statLang = "gui.dmz.stats.stamina";
                    case "Ki Damage" -> statLang = "gui.dmz.stats.ki_damage";
                    case "Max Ki" -> statLang = "gui.dmz.stats.max_ki";
                }
                Component statComponent = Component.translatable(statLang)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colores)).withBold(true));
                if (statKey.equals("Stamina")) {
                    graphics.drawString(font, statComponent, anchoTexto + 4, yOffset, colores);
                } else {
                    graphics.drawString(font, statComponent, anchoTexto, yOffset, colores);
                }
                // Dibujar Hovers
                if (mouseX >= anchoTexto -10 && mouseX <= anchoTexto + 60 && mouseY >= yOffset && mouseY <= yOffset + font.lineHeight) {
                    List<FormattedCharSequence> descriptionLines;
                    if (statKey.equals("Ki Damage")) {
                        descriptionLines = getStatDescription("ki_damage", font);
                    } else if (statKey.equals("Max Ki")) {
                        descriptionLines = getStatDescription("max_ki", font);
                    } else {
                        descriptionLines = getStatDescription(statKey.toLowerCase(), font);
                    }
                    graphics.renderTooltip(font, descriptionLines, mouseX, mouseY);
                }
            }
            anchoTexto = (this.width/2+75);
            //Efectos
            var majinOn = playerstats.hasDMZPermaEffect("majin"); var frutaOn = playerstats.hasDMZTemporalEffect("mightfruit");
            //Datos
            var transf = playerstats.getStringValue("form");

            var strMax = dmzdatos.calcMenuStrength(playerstats);
            var defMax = dmzdatos.calcMenuDefense(playerstats, Minecraft.getInstance().player);
            var conMax = dmzdatos.calcMenuConstitution(playerstats);
            var stmMax = dmzdatos.calcMenuStamina(playerstats);
            var KPWMax = dmzdatos.calcMenuKiPower(playerstats);
            var enrMax = dmzdatos.calcMenuEnergy(playerstats);

            var colorEnForma = majinOn || frutaOn || !Objects.equals(transf, "base") || playerstats.getIntValue("race") == 4 ? 0xfebc0d : 0xFFD7AB;

            drawStringWithBorder(graphics, font, Component.literal(numberFormatter.format(strMax)), anchoTexto+8, alturaTexto, colorEnForma);
            drawStringWithBorder(graphics, font, Component.literal(numberFormatter.format(defMax)), anchoTexto+8, alturaTexto + 12, colorEnForma);
            drawStringWithBorder(graphics, font, Component.literal(numberFormatter.format(conMax)), anchoTexto+8, alturaTexto + 24, 0xFFD7AB);
            drawStringWithBorder(graphics, font, Component.literal(numberFormatter.format(stmMax)), anchoTexto+8, alturaTexto + 36, 0xFFD7AB);
            drawStringWithBorder(graphics, font, Component.literal(numberFormatter.format(KPWMax)), anchoTexto+8, alturaTexto + 48, colorEnForma);
            drawStringWithBorder(graphics, font, Component.literal(numberFormatter.format(enrMax)), anchoTexto+8, alturaTexto + 60, 0xFFD7AB);

            var MultiTotal = Math.round((dmzdatos.calcTotalMultiplier(playerstats)) * 100) / 100.0;

            var multiMajin = DMZClientConfig.getMajin_multi();
            var multiFruta = DMZClientConfig.getTree_might_multi();
            var multiTransf = dmzdatos.calcularMultiTransf(playerstats);
            var anchoMulti = (this.width /2+2) - 3; var altoMulti = (this.height / 2) + 55;

            Component statComponent = Component.translatable("gui.dmz.stats.multiplier")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xC51D1D)).withBold(true));

            graphics.drawString(font, statComponent, anchoMulti, altoMulti, 0xC51D1D);

            if (mouseX >= anchoMulti -30 && mouseX <= anchoMulti + 50 && mouseY >= altoMulti && mouseY <= altoMulti + font.lineHeight) {
                List<FormattedCharSequence> descriptionLines = new ArrayList<>();
                descriptionLines.add(Component.translatable("stats.dmz.multiplier", MultiTotal).withStyle(ChatFormatting.BLUE).getVisualOrderText());
                descriptionLines.add(Component.translatable("stats.dmz.multiplier.desc", multiTransf).getVisualOrderText());
                descriptionLines.add(Component.translatable("stats.dmz.multi.transf", multiTransf).withStyle(ChatFormatting.DARK_AQUA).getVisualOrderText());
                if (majinOn) {
                    descriptionLines.add(Component.translatable("stats.dmz.multi.majin", multiMajin).withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText());
                }
                if (frutaOn) {
                    descriptionLines.add(Component.translatable("stats.dmz.multi.fruta", multiFruta).withStyle(ChatFormatting.RED).getVisualOrderText());
                }
                // Agregar más if luego para ver si está el Kaioken, etc, etc, etc.
                graphics.renderTooltip(font, descriptionLines, mouseX, mouseY);
            }
            drawStringWithBorder2(graphics, font, Component.literal("x"+MultiTotal), anchoTexto+6, alturaTexto + 80, colorEnForma);
        });
    }

    public void menuPaneles(GuiGraphics guiGraphics){
        //INFO GENERAL (Fuerza maxima, energia maxima, stamina, etc)
        alturaTexto = (this.height - 168)/2; anchoTexto = (this.width - 250)/2;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(menucentro, anchoTexto, alturaTexto, 0, 0, 250, 168);
        RenderSystem.disableBlend();

    }

    public static void drawStringWithBorder(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto, int ColorBorde) {
        // Calcular la posición centrada
        int textWidth = font.width(texto); int centeredX = x - (textWidth / 2);
        // Dibujar el texto con el borde
        guiGraphics.drawString(font, texto, centeredX + 1, y, ColorBorde, false);
        guiGraphics.drawString(font, texto, centeredX - 1, y, ColorBorde, false);
        guiGraphics.drawString(font, texto, centeredX, y + 1, ColorBorde, false);
        guiGraphics.drawString(font, texto, centeredX, y - 1, ColorBorde, false);
        guiGraphics.drawString(font, texto, centeredX, y, ColorTexto);
    }

    public static void drawStringWithBorder2(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto, int ColorBorde) {
        guiGraphics.drawString(font, texto, x + 1, y, ColorBorde, false);
        guiGraphics.drawString(font, texto, x - 1, y, ColorBorde, false);
        guiGraphics.drawString(font, texto, x, y + 1, ColorBorde, false);
        guiGraphics.drawString(font, texto, x, y - 1, ColorBorde, false);
        guiGraphics.drawString(font, texto, x, y, ColorTexto, false);
    }

    public static void drawStringWithBorder(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto) {
        drawStringWithBorder(guiGraphics, font, texto, x, y, ColorTexto, 0);
    }
    public static void drawStringWithBorder2(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto) {
        drawStringWithBorder2(guiGraphics, font, texto, x, y, ColorTexto, 0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<FormattedCharSequence> getStatDescription(String statKey, Font font) {
        Component descripcion = Component.translatable("stats.dmz." + statKey);
        int maxWidth = 200;
        return font.split(descripcion, maxWidth);
    }

    private String obtenerFormaLang(String forma, int race, String gender){
        return switch (race) {
            case 0 -> ("forms.dmz.human." + forma);
            case 1 -> ("forms.dmz.saiyan." + forma);
            case 2 -> ("forms.dmz.namek." + forma);
            case 3 -> ("forms.dmz.bioandroid." + forma);
            case 4 -> ("forms.dmz.colddemon." + forma);
            case 5 -> {
                String result = "";
                if (forma.equals("super") || forma.equals("ultra")) {
                    if (gender.equals("female")) result = ("forms.dmz.majin.female." + forma);
                    if (gender.equals("male")) result = ("forms.dmz.majin.male." + forma);
                } else result = ("forms.dmz.majin." + forma);
                yield result;
            }
            default ->("forms.dmz.human.base");
        };
    }
}
