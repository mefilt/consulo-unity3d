/*
 * Copyright 2013-2015 must-be.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mustbe.consulo.unity3d.ide.ui;

import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionChangeListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.unity3d.Unity3dBundle;
import org.mustbe.consulo.unity3d.module.Unity3dModuleExtensionUtil;
import org.mustbe.consulo.unity3d.module.Unity3dRootModuleExtension;
import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;

/**
 * @author VISTALL
 * @since 29.07.2015
 */
public class SetupUnitySDKProvider extends EditorNotifications.Provider<EditorNotificationPanel>
{
	private static final Key<EditorNotificationPanel> KEY = Key.create("setup.unity.sdk.notifier");

	private final Project myProject;

	public SetupUnitySDKProvider(Project project, final EditorNotifications notifications)
	{
		myProject = project;
		myProject.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter()
		{
			@Override
			public void rootsChanged(ModuleRootEvent event)
			{
				notifications.updateAllNotifications();
			}
		});
		myProject.getMessageBus().connect().subscribe(ModuleExtension.CHANGE_TOPIC, new ModuleExtensionChangeListener()
		{
			@Override
			public void beforeExtensionChanged(@NotNull ModuleExtension<?> oldExtension, @NotNull ModuleExtension<?> newExtension)
			{
				notifications.updateAllNotifications();
			}
		});
	}

	@Override
	public Key<EditorNotificationPanel> getKey()
	{
		return KEY;
	}

	@Override
	@RequiredReadAction
	public EditorNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor)
	{
		final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
		if(psiFile == null)
		{
			return null;
		}

		Unity3dRootModuleExtension rootModuleExtension = Unity3dModuleExtensionUtil.getRootModuleExtension(myProject);
		if(rootModuleExtension == null)
		{
			return null;
		}
		if(rootModuleExtension.getSdk() == null)
		{
			return createPanel(rootModuleExtension.getInheritableSdk().isNull() ? null : rootModuleExtension.getInheritableSdk()
					.getName(), rootModuleExtension.getModule());
		}
		return null;
	}

	@NotNull
	private static EditorNotificationPanel createPanel(@Nullable String name, @NotNull final Module rootModule)
	{
		EditorNotificationPanel panel = new EditorNotificationPanel();
		if(StringUtil.isEmpty(name))
		{
			panel.setText(Unity3dBundle.message("unity.sdk.is.not.defiled"));
		}
		else
		{
			panel.setText(Unity3dBundle.message("unity.0.sdk.is.not.defined", name));
		}
		panel.createActionLabel("Open Settings", new Runnable()
		{
			@Override
			@RequiredDispatchThread
			public void run()
			{

				ApplicationManager.getApplication().runWriteAction(new Runnable()
				{
					@Override
					public void run()
					{
						ProjectSettingsService.getInstance(rootModule.getProject()).openModuleSettings(rootModule);
					}
				});
			}
		});
		return panel;
	}
}
