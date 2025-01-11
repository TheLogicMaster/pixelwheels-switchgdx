/*
 * Copyright 2017 Aurélien Gâteau <mail@agateau.com>
 *
 * This file is part of Pixel Wheels.
 *
 * Pixel Wheels is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.agateau.pixelwheels.screens;

import static com.agateau.translations.Translator.tr;
import static com.agateau.translations.Translator.trc;

import com.agateau.pixelwheels.Constants;
import com.agateau.pixelwheels.GameConfig;
import com.agateau.pixelwheels.Language;
import com.agateau.pixelwheels.LogExporter;
import com.agateau.pixelwheels.LogExporterUtils;
import com.agateau.pixelwheels.PwGame;
import com.agateau.pixelwheels.PwRefreshHelper;
import com.agateau.pixelwheels.VersionInfo;
import com.agateau.pixelwheels.gameinput.GameInputHandlerFactory;
import com.agateau.pixelwheels.screens.config.InputSelectorController;
import com.agateau.pixelwheels.utils.StringUtils;
import com.agateau.ui.anchor.AnchorGroup;
import com.agateau.ui.menu.ButtonMenuItem;
import com.agateau.ui.menu.LabelMenuItem;
import com.agateau.ui.menu.Menu;
import com.agateau.ui.menu.MenuItemGroup;
import com.agateau.ui.menu.MenuItemListener;
import com.agateau.ui.menu.SelectorMenuItem;
import com.agateau.ui.menu.SwitchMenuItem;
import com.agateau.ui.menu.TabMenuItem;
import com.agateau.ui.uibuilder.UiBuilder;
import com.agateau.utils.FileUtils;
import com.agateau.utils.PlatformUtils;
import com.agateau.utils.log.NLog;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** The config screen */
public class ConfigScreen extends PwStageScreen {
    private final PwGame mGame;
    private final Origin mOrigin;

    private static final String WEBSITE_URL = "https://agateau.com/projects/pixelwheels";

    /// Indicates from where has this config screen been called from
    public enum Origin {
        MENU,
        PAUSE_OVERLAY
    }

    Menu mMenu;
    TabMenuItem mTabMenuItem;
    MenuItemGroup mLanguageGroup;

    private boolean mLanguageChanged = false;

    public ConfigScreen(PwGame game, Origin origin) {
        super(game.getAssets().ui);
        mGame = game;
        mOrigin = origin;
        setupUi();
        new PwRefreshHelper(mGame, getStage()) {
            @Override
            protected void refresh() {
                mGame.replaceScreen(new ConfigScreen(mGame, mOrigin));
            }
        };
    }

    private void setupUi() {
        UiBuilder builder = new UiBuilder(mGame.getAssets().atlas, mGame.getAssets().ui.skin);

        AnchorGroup root = (AnchorGroup) builder.build(FileUtils.assets("screens/config.gdxui"));
        root.setFillParent(true);
        getStage().addActor(root);

        mMenu = builder.getActor("menu");

        mTabMenuItem = new TabMenuItem(mMenu);
        mMenu.addItem(mTabMenuItem);

        addAudioVideoTab();
        addControlsTab();
        addAboutTab();
        addInternalTab();

        mMenu.addBackButton()
                .addListener(
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                onBackPressed();
                            }
                        });
    }

    private void addAboutTab() {
        MenuItemGroup group = mTabMenuItem.addPage(tr("About"));
        group.setWidth(800);
        group.addLabel(StringUtils.format(tr("Pixel Wheels %s"), VersionInfo.VERSION));
        group.addButton(tr("CREDITS"))
                .setParentWidthRatio(0.5f)
                .addListener(
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                mGame.pushScreen(new CreditsScreen(mGame));
                            }
                        });

        group.addButton(tr("WEB SITE"))
                .setParentWidthRatio(0.5f)
                .addListener(
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                PlatformUtils.openURI(WEBSITE_URL);
                            }
                        });

        group.addSpacer();
    }

    private void addInternalTab() {
        MenuItemGroup group = mTabMenuItem.addPage(tr("Under the hood"));
        group.setWidth(800);

        LogExporter logExporter = mGame.getLogExporter();
        if (logExporter != null) {
            group.addLabel(logExporter.getDescription()).setWrap(true);
            group.addButton(logExporter.getActionText())
                    .setParentWidthRatio(0.5f)
                    .addListener(
                            new MenuItemListener() {
                                @Override
                                public void triggered() {
                                    LogExporterUtils.exportLogs(logExporter);
                                }
                            });
            group.addSpacer();
        }

        group.addLabel(
                        tr(
                                "These options are mostly interesting for Pixel Wheels development, but feel free to poke around!"))
                .setWrap(true);

        group.addButton(tr("DEV. OPTIONS"))
                .setParentWidthRatio(0.5f)
                .addListener(
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                mGame.pushScreen(new DebugScreen(mGame));
                            }
                        });
    }

    private void addControlsTab() {
        MenuItemGroup group = mTabMenuItem.addPage(tr("Controls"));
        group.setWidth(750);

        if (PlatformUtils.isDesktop()) {
            TabMenuItem tabMenuItem = new TabMenuItem(mMenu);
            group.addItem(tabMenuItem);
            for (int idx = 0; idx < Constants.MAX_PLAYERS; ++idx) {
                String tabText = StringUtils.format(tr("P%d"), idx + 1);
                MenuItemGroup playerGroup = tabMenuItem.addPage(tabText);
                String selectorText = StringUtils.format(tr("Player #%d:"), idx + 1);
                setupInputSelector(mMenu, playerGroup, selectorText, idx);
            }
        } else {
            setupInputSelector(mMenu, group, tr("Controls:"), 0);
        }
    }

    private void addAudioVideoTab() {
        final GameConfig gameConfig = mGame.getConfig();
        MenuItemGroup group = mTabMenuItem.addPage(tr("General"));
        mLanguageGroup = group;

        ButtonMenuItem languageButton = new ButtonMenuItem(mMenu, getLanguageText());
        languageButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        mGame.pushScreen(new SelectLanguageScreen(mGame));
                    }
                });
        group.addItemWithLabel(tr("Language:"), languageButton);
        if (mOrigin == Origin.PAUSE_OVERLAY) {
            languageButton.setDisabled(true);
            languageButton.setText(
                    trc("Can't change while racing", "'change' refer to changing languages"));
        }

        group.addTitleLabel(tr("Audio"));
        final SwitchMenuItem soundFxSwitch = new SwitchMenuItem(mMenu);
        soundFxSwitch.setChecked(gameConfig.playSoundFx);
        soundFxSwitch
                .getActor()
                .addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                gameConfig.playSoundFx = soundFxSwitch.isChecked();
                                gameConfig.flush();
                            }
                        });
        group.addItemWithLabel(tr("Sound FX:"), soundFxSwitch);

        final SwitchMenuItem musicSwitch = new SwitchMenuItem(mMenu);
        musicSwitch.setChecked(gameConfig.playMusic);
        musicSwitch
                .getActor()
                .addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                gameConfig.playMusic = musicSwitch.isChecked();
                                gameConfig.flush();
                            }
                        });
        group.addItemWithLabel(tr("Music:"), musicSwitch);

        group.addTitleLabel(tr("Video"));
//        if (PlatformUtils.isDesktop()) {
//            final SwitchMenuItem fullscreenSwitch = new SwitchMenuItem(mMenu);
//            fullscreenSwitch.setChecked(gameConfig.fullscreen);
//            fullscreenSwitch
//                    .getActor()
//                    .addListener(
//                            new ChangeListener() {
//                                @Override
//                                public void changed(ChangeEvent event, Actor actor) {
//                                    gameConfig.fullscreen = fullscreenSwitch.isChecked();
//                                    mGame.setFullscreen(gameConfig.fullscreen);
//                                    gameConfig.flush();
//                                }
//                            });
//            group.addItemWithLabel(tr("Fullscreen:"), fullscreenSwitch);
//        }

        final SwitchMenuItem headingUpCameraSwitch = new SwitchMenuItem(mMenu);
        headingUpCameraSwitch.setChecked(gameConfig.headingUpCamera);
        headingUpCameraSwitch
                .getActor()
                .addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                gameConfig.headingUpCamera = headingUpCameraSwitch.isChecked();
                                gameConfig.flush();
                            }
                        });
        group.addItemWithLabel(tr("Rotate camera:"), headingUpCameraSwitch);
    }

    private String getLanguageText() {
        final GameConfig gameConfig = mGame.getConfig();
        Language language = mGame.getAssets().languages.getLanguage(gameConfig.languageId);
        return language.name;
    }

    private void selectLanguageButton() {
        mTabMenuItem.setCurrentPage(mLanguageGroup);
        mMenu.setCurrentItem(mLanguageGroup);
    }

    public static ConfigScreen createAfterLanguageChange(PwGame game) {
        ConfigScreen screen = new ConfigScreen(game, Origin.MENU);
        screen.selectLanguageButton();
        screen.mLanguageChanged = true;
        return screen;
    }

    private void setupInputSelector(Menu menu, MenuItemGroup group, String label, final int idx) {
        SelectorMenuItem<GameInputHandlerFactory> selector = new SelectorMenuItem<>(menu);
        group.addItemWithLabel(label, selector);

        ButtonMenuItem configureButton = new ButtonMenuItem(menu, tr("CONFIGURE"));
        group.addItemWithLabel("", configureButton);

        LabelMenuItem nameLabel = new LabelMenuItem("", menu.getSkin());
        group.addItemWithLabel("", nameLabel);

        InputSelectorController controller =
                new InputSelectorController(mGame, selector, configureButton, nameLabel, idx);
        controller.setStartupState();
    }

    @Override
    public void onBackPressed() {
        mGame.popScreen();
        if (mLanguageChanged) {
            NLog.i("Language changed, recreating MainMenuScreen");
            mGame.showMainMenu();
        }
    }
}
