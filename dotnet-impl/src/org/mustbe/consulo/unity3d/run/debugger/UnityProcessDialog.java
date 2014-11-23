package org.mustbe.consulo.unity3d.run.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.unity3d.Unity3dIcons;
import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

/**
 * @author VISTALL
 * @since 18.11.14
 */
public class UnityProcessDialog extends ChooseElementsDialog<UnityPlayer>
{
	private UnityPlayerService.UpdateListener myListener;

	public UnityProcessDialog(@NotNull Project project)
	{
		super(project, new ArrayList<UnityPlayer>(UnityPlayerService.getInstance().getPlayers()), "Select Unity Player", "", true);

		myListener = new UnityPlayerService.UpdateListener()
		{
			@Override
			public void update(@NotNull final List<UnityPlayer> unityPlayers)
			{
				UIUtil.invokeLaterIfNeeded(new Runnable()
				{
					@Override
					public void run()
					{
						UnityProcessDialog.this.setElements(unityPlayers, Collections.<UnityPlayer>emptyList());
					}
				});
			}
		};

		UnityPlayerService.getInstance().addUpdateListener(myListener);
	}

	@Nullable
	@Override
	protected String getDimensionServiceKey()
	{
		return getClass().getSimpleName();
	}

	@Override
	protected void dispose()
	{
		UnityPlayerService.getInstance().removeUpdateListener(myListener);
		super.dispose();
	}

	@Override
	protected String getItemText(UnityPlayer item)
	{
		return item.getId() + " (" + item.getIp() + ":" + item.getDebuggerPort() + ")";
	}

	@Nullable
	@Override
	protected Icon getItemIcon(UnityPlayer item)
	{
		return Unity3dIcons.Unity3d;
	}
}