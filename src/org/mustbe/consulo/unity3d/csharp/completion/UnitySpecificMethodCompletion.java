/*
 * Copyright 2013-2014 must-be.org
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

package org.mustbe.consulo.unity3d.csharp.completion;

import static com.intellij.patterns.StandardPatterns.psiElement;

import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.csharp.CSharpIcons;
import org.mustbe.consulo.csharp.lang.psi.CSharpMethodDeclaration;
import org.mustbe.consulo.csharp.lang.psi.CSharpTypeDeclaration;
import org.mustbe.consulo.csharp.lang.psi.CSharpTypeRefPresentationUtil;
import org.mustbe.consulo.csharp.lang.psi.impl.source.CSharpBlockStatementImpl;
import org.mustbe.consulo.csharp.lang.psi.impl.source.resolve.type.CSharpTypeRefByQName;
import org.mustbe.consulo.dotnet.psi.DotNetInheritUtil;
import org.mustbe.consulo.dotnet.psi.DotNetQualifiedElement;
import org.mustbe.consulo.dotnet.psi.DotNetStatement;
import org.mustbe.consulo.dotnet.psi.DotNetVirtualImplementOwner;
import org.mustbe.consulo.unity3d.Unity3dIcons;
import org.mustbe.consulo.unity3d.csharp.UnityFunctionManager;
import org.mustbe.consulo.unity3d.csharp.UnityTypes;
import org.mustbe.consulo.unity3d.module.Unity3dModuleExtension;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IconDescriptor;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

/**
 * @author VISTALL
 * @since 19.12.14
 * <p/>
 * Variant of {@link org.mustbe.consulo.csharp.ide.completion.CSharpOverrideOrImplementCompletionContributor}
 */
public class UnitySpecificMethodCompletion extends CompletionContributor
{
	public UnitySpecificMethodCompletion()
	{
		extend(CompletionType.BASIC, psiElement().withSuperParent(4, CSharpTypeDeclaration.class), new CompletionProvider<CompletionParameters>()
		{
			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
			{
				DotNetQualifiedElement currentElement = PsiTreeUtil.getParentOfType(parameters.getPosition(), DotNetQualifiedElement.class);
				assert currentElement != null;
				if(!currentElement.getText().contains(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
				{
					return;
				}

				CSharpTypeDeclaration typeDeclaration = PsiTreeUtil.getParentOfType(parameters.getPosition(), CSharpTypeDeclaration.class);
				assert typeDeclaration != null;

				Unity3dModuleExtension extension = ModuleUtilCore.getExtension(typeDeclaration, Unity3dModuleExtension.class);
				if(extension == null)
				{
					return;
				}

				if(!DotNetInheritUtil.isParent(UnityTypes.UnityEngine.MonoBehaviour, typeDeclaration, true))
				{
					return;
				}

				Collection<UnityFunctionManager.FunctionInfo> functionInfos = UnityFunctionManager.getInstance().getFunctionInfos();
				for(UnityFunctionManager.FunctionInfo functionInfo : functionInfos)
				{
					UnityFunctionManager.FunctionInfo nonParameterListCopy = functionInfo.createNonParameterListCopy();
					if(nonParameterListCopy != null)
					{
						result.addElement(buildLookupItem(nonParameterListCopy, typeDeclaration));
					}

					result.addElement(buildLookupItem(functionInfo, typeDeclaration));
				}
			}
		});
	}

	@NotNull
	private static LookupElementBuilder buildLookupItem(UnityFunctionManager.FunctionInfo functionInfo, CSharpTypeDeclaration scope)
	{
		StringBuilder builder = new StringBuilder();

		builder.append("void ");
		builder.append(functionInfo.getName());
		builder.append("(");

		boolean first = true;
		for(Map.Entry<String, String> entry : functionInfo.getParameters().entrySet())
		{
			if(first)
			{
				first = false;
			}
			else
			{
				builder.append(", ");
			}

			builder.append(CSharpTypeRefPresentationUtil.buildText(new CSharpTypeRefByQName(entry.getValue()), scope,
					CSharpTypeRefPresentationUtil.TYPE_KEYWORD));
			builder.append(" ");
			builder.append(entry.getKey());
		}
		builder.append(")");

		String presentationText = builder.toString();
		builder.append("{\n");
		builder.append("}");

		LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(builder.toString());
		lookupElementBuilder = lookupElementBuilder.withPresentableText(presentationText);
		lookupElementBuilder = lookupElementBuilder.withLookupString(functionInfo.getName());
		lookupElementBuilder = lookupElementBuilder.withTailText("{...}", true);

		IconDescriptor iconDescriptor = new IconDescriptor(new IconDescriptor(AllIcons.Nodes.Method).addLayerIcon(CSharpIcons.Lang).toIcon());
		iconDescriptor.setRightIcon(Unity3dIcons.EventMethod);

		lookupElementBuilder = lookupElementBuilder.withIcon(iconDescriptor.toIcon());

		lookupElementBuilder = lookupElementBuilder.withInsertHandler(new InsertHandler<LookupElement>()
		{
			@Override
			public void handleInsert(InsertionContext context, LookupElement item)
			{
				CaretModel caretModel = context.getEditor().getCaretModel();

				PsiElement elementAt = context.getFile().findElementAt(caretModel.getOffset() - 1);
				if(elementAt == null)
				{
					return;
				}

				DotNetVirtualImplementOwner virtualImplementOwner = PsiTreeUtil.getParentOfType(elementAt, DotNetVirtualImplementOwner.class);
				if(virtualImplementOwner == null)
				{
					return;
				}

				if(virtualImplementOwner instanceof CSharpMethodDeclaration)
				{
					PsiElement codeBlock = ((CSharpMethodDeclaration) virtualImplementOwner).getCodeBlock();
					if(codeBlock instanceof CSharpBlockStatementImpl)
					{
						DotNetStatement[] statements = ((CSharpBlockStatementImpl) codeBlock).getStatements();
						if(statements.length > 0)
						{
							caretModel.moveToOffset(statements[0].getTextOffset() + statements[0].getTextLength());
						}
						else
						{
							caretModel.moveToOffset(((CSharpBlockStatementImpl) codeBlock).getLeftBrace().getTextOffset() + 1);
						}
					}
				}

				context.commitDocument();

				CodeStyleManager.getInstance(context.getProject()).reformat(virtualImplementOwner);
			}
		});
		return lookupElementBuilder;
	}
}
