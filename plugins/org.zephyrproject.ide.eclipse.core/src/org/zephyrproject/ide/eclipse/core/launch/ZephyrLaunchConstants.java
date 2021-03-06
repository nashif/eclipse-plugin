/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launch;

import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;

/**
 * Contains launch related constants.
 */
public final class ZephyrLaunchConstants {

	public static final String LAUNCH_ID =
			ZephyrPlugin.PLUGIN_ID + ".ui.launch"; //$NON-NLS-1$

	public static final String ATTR_EMULATOR_RUN_CMD_SEL =
			LAUNCH_ID + ".EMULATOR_RUN_CMD_SEL"; //$NON-NLS-1$

	public static final String EMULATOR_RUN_CMD_SEL_NONE =
			"EMULATOR_RUN_CMD_SEL_NONE"; //$NON-NLS-1$

	public static final String EMULATOR_RUN_CMD_SEL_BUILDSYS =
			"EMULATOR_RUN_CMD_SEL_BUILDSYS"; //$NON-NLS-1$

	public static final String EMULATOR_RUN_CMD_SEL_WEST =
			"EMULATOR_RUN_CMD_SEL_WEST"; //$NON-NLS-1$

	public static final String ATTR_RUN_CMD_WEST_ARGS =
			LAUNCH_ID + ".RUN_CMD_WEST_ARGS"; //$NON-NLS-1$

	public static final String ATTR_FLASH_CMD_SEL =
			LAUNCH_ID + ".FLASH_CMD_SEL"; //$NON-NLS-1$

	public static final String FLASH_CMD_SEL_NONE = "FLASH_CMD_SEL_NONE"; //$NON-NLS-1$

	public static final String FLASH_CMD_SEL_BUILDSYS =
			"FLASH_CMD_SEL_BUILDSYS"; //$NON-NLS-1$

	public static final String FLASH_CMD_SEL_WEST = "FLASH_CMD_SEL_WEST"; //$NON-NLS-1$

	public static final String ATTR_FLASH_CMD_WEST_ARGS =
			LAUNCH_ID + ".FLASH_CMD_WEST_ARGS"; //$NON-NLS-1$

	public static final String ATTR_DBGSERVER_CMD_SEL =
			LAUNCH_ID + ".DBGSERVER_CMD_SEL"; //$NON-NLS-1$

	public static final String DBGSERVER_CMD_SEL_NONE =
			"DBGSERVER_CMD_SEL_NONE"; //$NON-NLS-1$

	public static final String DBGSERVER_CMD_SEL_BUILDSYS =
			"DBGSERVER_CMD_SEL_BUILDSYS"; //$NON-NLS-1$

	public static final String DBGSERVER_CMD_SEL_WEST =
			"DBGSERVER_CMD_SEL_WEST"; //$NON-NLS-1$

	public static final String ATTR_DBGSERVER_CMD_WEST_ARGS =
			LAUNCH_ID + ".DBGSERVER_CMD_WEST_ARGS"; //$NON-NLS-1$

	public static final String LAUNCH_TARGET_EMULATOR_RUN_NAME = "Emulator"; // $NON-NLS-1

	public static final String LAUNCH_TARGET_HARDWARE_RUN_NAME = "Hardware"; // $NON-NLS-1

	public static final String LAUNCH_TARGET_EMULATOR_RUN_TYPE_ID =
			ZephyrPlugin.PLUGIN_ID + ".launchTargetType.emulator.run"; //$NON-NLS-1$

	public static final String LAUNCH_TARGET_HARDWARE_RUN_TYPE_ID =
			ZephyrPlugin.PLUGIN_ID + ".launchTargetType.hardware.run"; //$NON-NLS-1$

	public static final String LAUNCH_TARGET_OS = "zephyr"; //$NON-NLS-1$

	public static final String LAUNCH_TARGET_ARCH = "zephyr"; //$NON-NLS-1$
}
