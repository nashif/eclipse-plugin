/*
 * Copyright (c) 2015, 2016 QNX Software Systems and others.
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfoChangeListener;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.internal.build.CMakeCache;

/**
 * CMake Generator Build Configuration
 *
 * This contains code to execute CMake.
 *
 * Originally from org.eclipse.cdt.cmake.core.internal.CMakeBuildConfiguration
 * and org.eclipse.cdt.core.build.CBuildConfiguration.
 *
 * @see org.eclipse.cdt.cmake.core.internal.CMakeBuildConfiguration
 * @see org.eclipse.cdt.core.build.CBuildConfiguration
 */
public class CMakeGeneratorBuildConfiguration extends PlatformObject
		implements ICBuildConfiguration {

	private IBuildConfiguration config;

	private ZephyrApplicationBuildConfiguration zAppBuildCfg;

	private ScopedPreferenceStore pStore;

	private final String cmakeCmd;

	public CMakeGeneratorBuildConfiguration(IBuildConfiguration config,
			ZephyrApplicationBuildConfiguration zAppBuildCfg) {
		this.config = config;
		this.zAppBuildCfg = zAppBuildCfg;

		this.pStore =
				ZephyrHelpers.getProjectPreferenceStore(config.getProject());

		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			cmakeCmd = "cmake.exe"; //$NON-NLS-1$
		} else {
			cmakeCmd = "cmake"; // $NON-NLS-1$
		}
	}

	/**
	 * Get the board name to be built for.
	 *
	 * @return Name of board to be built for.
	 */
	private String getBoardName() {
		return ZephyrHelpers.getBoardName(pStore);
	}

	/**
	 * @return The CMake Generator identifier
	 */
	private String getCMakeGenerator() {
		String generator = pStore.getString(CMakeConstants.CMAKE_GENERATOR);

		if (generator.trim().isEmpty()) {
			return ZephyrHelpers.getDefaultCMakeGenerator();
		}

		return generator;
	}

	protected int watchProcess(Process process, IConsoleParser[] consoleParsers,
			IConsole console) throws CoreException {
		new ReaderThread(process.getInputStream(), consoleParsers,
				console.getOutputStream()).start();
		new ReaderThread(process.getErrorStream(), consoleParsers,
				console.getErrorStream()).start();
		try {
			return process.waitFor();
		} catch (InterruptedException e) {
			CCorePlugin.log(e);
			return -1;
		}
	}

	private static class ReaderThread extends Thread {

		private final BufferedReader in;
		private final PrintStream out;
		private final IConsoleParser[] consoleParsers;

		public ReaderThread(InputStream in, IConsoleParser[] consoleParsers,
				OutputStream out) {
			this.in = new BufferedReader(new InputStreamReader(in));
			this.consoleParsers = consoleParsers;
			this.out = new PrintStream(out);
		}

		@Override
		public void run() {
			try {
				for (String line = in.readLine(); line != null; line =
						in.readLine()) {
					for (IConsoleParser consoleParser : consoleParsers) {
						// Synchronize to avoid interleaving of lines
						synchronized (consoleParser) {
							consoleParser.processLine(line);
						}
					}
					out.println(line);
				}
			} catch (IOException e) {
				CCorePlugin.log(e);
			}
		}

	}

	@Override
	public void setBuildEnvironment(Map<String, String> env) {
		ZephyrHelpers.setupBuildCommandEnvironment(pStore, env);
		CCorePlugin.getDefault().getBuildEnvironmentManager()
				.setEnvironment(env, config, true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.core.build.ICBuildConfiguration#build(int,
	 * java.util.Map, org.eclipse.cdt.core.resources.IConsole,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IProject[] build(int kind, Map<String, String> args,
			IConsole console, IProgressMonitor monitor) throws CoreException {
		IProject project = config.getProject();

		IPath prjIPath = project.getLocation();

		/* Make sure CMakeLists.txt is there */
		if (!prjIPath.append("CMakeLists.txt").toFile().exists()) { //$NON-NLS-1$
			throw new CoreException(ZephyrHelpers.errorStatus(
					"File CMakeLists.txt does not exist under project root", //$NON-NLS-1$
					null));
		}

		/* Make sure prj.conf is there */
		if (!prjIPath.append("prj.conf").toFile().exists()) { //$NON-NLS-1$
			throw new CoreException(ZephyrHelpers.errorStatus(
					"File prj.txt does not exist under project root", null));//$NON-NLS-1$
		}

		try {
			/* Remove C-related warnings/errors */
			project.deleteMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, false,
					IResource.DEPTH_INFINITE);

			ConsoleOutputStream consoleOut = console.getOutputStream();

			Path buildDir = zAppBuildCfg.getBuildDirectory();
			IFolder buildFolder = (IFolder) zAppBuildCfg.getBuildContainer();

			String boardName = getBoardName();

			String projectAbsPath =
					new File(project.getLocationURI()).getAbsolutePath();

			if (!Files.exists(buildDir.resolve("CMakeFiles")) //$NON-NLS-1$
					|| (kind == IncrementalProjectBuilder.FULL_BUILD)) {

				consoleOut.write(String.format(
						"----- Generating CMake files for board %s in %s%n",
						boardName,
						buildFolder.getProjectRelativePath().toString()));

				List<String> command = new ArrayList<>();

				Path cmakePath = zAppBuildCfg.findCommand(cmakeCmd); // $NON-NLS-1$
				if (cmakePath != null) {
					command.add(cmakePath.toString());
				} else {
					/*
					 * CBuildConfiguration.findCommand() looks for "PATH" in
					 * the environment to prepend paths. However, Windows
					 * uses "Path" (not all caps), so findCommand() would fail.
					 * So assume the environment has been setup correctly
					 * and cmake can be called anywhere here.
					 */
					command.add(cmakeCmd);
				}

				command.add(String.format("-DBOARD=%s", boardName)); //$NON-NLS-1$

				command.add("-G"); //$NON-NLS-1$
				command.add(getCMakeGenerator());

				command.add("-DCMAKE_EXPORT_COMPILE_COMMANDS=ON"); //$NON-NLS-1$

				command.add(projectAbsPath);

				ProcessBuilder processBuilder = new ProcessBuilder(command)
						.directory(buildDir.toFile());
				setBuildEnvironment(processBuilder.environment());
				Process process = processBuilder.start();
				consoleOut.write(
						String.join(" ", command) + System.lineSeparator());

				watchProcess(process, new IConsoleParser[0], console);

				consoleOut.write(String.format(
						"----- Done generating CMake files for board %s in %s%n",
						boardName,
						buildFolder.getProjectRelativePath().toString()));

				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}

			/*
			 * For previously created projects without those CMake variables
			 * saved into project preference space, do this here to jumpstart
			 * the process. Or else the actual build would fail as those
			 * variables would result in NullPointerException inside
			 * ZephyrApplicationBuildConfiguration.
			 */
			parseCMakeCache(project, buildDir);

			monitor.worked(1);
			monitor.done();

			/*
			 * Return this project so that we get resource delta next time
			 * this is invoked.
			 */
			return new IProject[] {
				project
			};
		} catch (IOException eio) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					String.format("Error invoking CMake for project %s!",
							project.getName()),
					eio));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.cdt.core.build.ICBuildConfiguration#clean(org.eclipse.cdt.
	 * core.resources.IConsole, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void clean(IConsole console, IProgressMonitor monitor)
			throws CoreException {
		/* TODO: maybe clean out cmake files? */
		IProject project = config.getProject();

		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}

	@Override
	public IScannerInfo getScannerInformation(IResource resource) {
		return null;
	}

	@Override
	public void subscribe(IResource resource,
			IScannerInfoChangeListener listener) {
	}

	@Override
	public void unsubscribe(IResource resource,
			IScannerInfoChangeListener listener) {
	}

	@Override
	public IBuildConfiguration getBuildConfiguration() throws CoreException {
		return config;
	}

	@Override
	public IToolChain getToolChain() throws CoreException {
		return null;
	}

	@Override
	public String getBinaryParserId() throws CoreException {
		return null;
	}

	@Override
	public IEnvironmentVariable getVariable(String name) throws CoreException {
		return null;
	}

	@Override
	public IEnvironmentVariable[] getVariables() throws CoreException {
		return null;
	}

	private void parseCMakeCache(IProject project, Path buildDir)
			throws IOException, CoreException {
		Path cachePath = buildDir.resolve("CMakeCache.txt"); //$NON-NLS-1$
		boolean done = false;
		if (Files.exists(cachePath)) {
			CMakeCache cache = new CMakeCache(project);
			done = cache.parseFile(cachePath.toFile());
			if (done) {
				ScopedPreferenceStore pStore =
						ZephyrHelpers.getProjectPreferenceStore(project);

				pStore.putValue(CMakeCache.CMAKE_C_COMPILER,
						cache.getCCompiler());
				pStore.putValue(CMakeCache.CMAKE_CXX_COMPILER,
						cache.getCXXCompiler());
				pStore.putValue(CMakeCache.CMAKE_MAKE_PROGRAM,
						cache.getMakeProgram());
				pStore.putValue(CMakeCache.CMAKE_GDB, cache.getGdb());

				pStore.putValue(CMakeCache.WEST, cache.getWest());

				String val = cache.getDebugRunner();
				if ((val != null) && !val.isEmpty()) {
					pStore.putValue(CMakeCache.ZEPHYR_BOARD_DEBUG_RUNNER,
							cache.getDebugRunner());
				}

				pStore.save();
			}
		}

		if (!done) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Build not configured properly.", new Exception()));
		}
	}
}
