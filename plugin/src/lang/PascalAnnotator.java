package com.siberika.idea.pascal.lang;

import com.intellij.lang.annotation.*;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.siberika.idea.pascal.PascalBundle;
import com.siberika.idea.pascal.editor.PascalActionDeclare;
import com.siberika.idea.pascal.editor.PascalRoutineActions;
import com.siberika.idea.pascal.editor.refactoring.PascalRenameAction;
import com.siberika.idea.pascal.ide.actions.AddFixType;
import com.siberika.idea.pascal.ide.actions.SectionToggle;
import com.siberika.idea.pascal.ide.actions.UsesActions;
import com.siberika.idea.pascal.lang.context.ContextUtil;
import com.siberika.idea.pascal.lang.parser.NamespaceRec;
import com.siberika.idea.pascal.lang.psi.*;
import com.siberika.idea.pascal.lang.psi.impl.PasExportedRoutineImpl;
import com.siberika.idea.pascal.lang.psi.impl.PasField;
import com.siberika.idea.pascal.lang.psi.impl.PasRoutineImplDeclImpl;
import com.siberika.idea.pascal.lang.psi.impl.PasVariantScope;
import com.siberika.idea.pascal.lang.references.ResolveContext;
import com.siberika.idea.pascal.lang.references.ResolveUtil;
import com.siberika.idea.pascal.lang.references.resolve.Resolve;
import com.siberika.idea.pascal.util.PsiContext;
import com.siberika.idea.pascal.util.PsiUtil;
import com.siberika.idea.pascal.util.StrUtil;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.siberika.idea.pascal.PascalBundle.message;

/**
 * Author: George Bakhtadze
 * Date: 12/14/12
 */
public class PascalAnnotator implements Annotator {

    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PasExportedRoutineImpl exportedRoutine) {
            annotateRoutineInInterface(exportedRoutine, holder);
        }
        if (element instanceof PasRoutineImplDeclImpl routineImpl) {
            annotateRoutineInImplementation(routineImpl, holder);
        }

        annotateModuleHead(element, holder);

        //noinspection ConstantConditions
        if (PsiUtil.isEntityName(element) && !PsiUtil.isLastPartOfMethodImplName((PascalNamedElement) element)) {
            //noinspection ConstantConditions
            PascalNamedElement namedElement = (PascalNamedElement) element;
            List<PsiElement> scopes = new SmartList<>();
            ResolveContext resolveContext = new ResolveContext(null, PasField.TYPES_ALL, true, scopes, null);

            final NamespaceRec fqn = NamespaceRec.fromElement(element);
            boolean noTargets = Resolve.resolveExpr(fqn, resolveContext, (originalScope, scope, field, type) -> false);

            if (noTargets && !isVariantField(scopes)) {
                AnnotationBuilder ann = holder.newAnnotation(HighlightSeverity.ERROR, message("ann.error.undeclared.identifier")).range(element);
                PsiContext context = PsiUtil.getContext(namedElement);
                Set<AddFixType> fixes = EnumSet.of(AddFixType.VAR, AddFixType.TYPE, AddFixType.CONST, AddFixType.ROUTINE, AddFixType.UNIT_FIND); // [*] => var type const routine
                if (context == PsiContext.FQN_FIRST) {
                    if (!ResolveUtil.findUnitsWithStub(namedElement.getProject(), ModuleUtil.findModuleForPsiElement(namedElement), namedElement.getName()).isEmpty()) {
                        fixes.add(AddFixType.UNIT);
                    }
                }
                Iterator<PsiElement> scopesIterators = scopes.iterator();
                PsiElement scope = scopesIterators.hasNext() ? scopesIterators.next() : null;
                // Skip WITH scopes
                boolean firstPart = true;
                if (fqn.getParentIdent() instanceof PasFullyQualifiedIdent) {
                    firstPart = (PsiContext.FQN_FIRST == context) || (PsiContext.FQN_SINGLE == context)
                            || (((PasFullyQualifiedIdent) fqn.getParentIdent()).getSubIdentList().size() == 1);
                }
                if (firstPart && (scope instanceof PascalStructType) && (scopesIterators.hasNext())) {
                    PasEntityScope nearest = PsiUtil.getNearestAffectingScope(element);
                    if (!(nearest instanceof PascalStructType)) {
                        while (scopesIterators.hasNext() && (scope instanceof PascalStructType)) {
                            scope = scopesIterators.next();
                        }
                    }
                }

                if (scope instanceof PasEnumType) {                                                          // TEnum.* => -* +enum
                    fixes = EnumSet.of(AddFixType.ENUM);
                    fixes.remove(AddFixType.UNIT_FIND);
                } else if (scope instanceof PascalRoutine) {                                                 // [inRoutine] => +parameter
                    fixes.add(AddFixType.PARAMETER);
                }
                if (context == PsiContext.TYPE_ID) {                                                         // [TypeIdent] => -* +type
                    fixes = EnumSet.of(AddFixType.TYPE, AddFixType.UNIT_FIND);
                } else if (PsiTreeUtil.getParentOfType(namedElement, PasConstExpression.class) != null) {    // [part of const expr] => -* +const +enum
                    fixes = EnumSet.of(AddFixType.CONST, AddFixType.UNIT_FIND);
                } else if (context == PsiContext.EXPORT) {
                    fixes = EnumSet.of(AddFixType.ROUTINE);
                } else if (context == PsiContext.CALL) {
                    fixes = EnumSet.of(AddFixType.ROUTINE, AddFixType.VAR, AddFixType.UNIT_FIND);
                } else if (context == PsiContext.PROPERTY_SPEC) {
                    fixes = EnumSet.of(AddFixType.VAR, AddFixType.ROUTINE);
                } else if (context == PsiContext.FOR) {
                    fixes = EnumSet.of(AddFixType.VAR, AddFixType.UNIT_FIND);
                }
                if (context == PsiContext.USES) {
                    fixes = EnumSet.of(AddFixType.NEW_UNIT);
                }

                String name = namedElement.getName();
                for (AddFixType fix : fixes) {
                    switch (fix) {
                        case VAR: {
                            boolean priority = context != PsiContext.CALL;
                            if (!(scope instanceof PascalStructType)) {
                                ann = ann.newFix(PascalActionDeclare.newActionCreateVar(message("action.create.var"), namedElement, null, priority, context != PsiContext.FOR ? null : "Integer")).registerFix();
                            }
                            PsiElement adjustedScope = adjustScope(scope);
                            if (adjustedScope instanceof PascalStructType) {
                                if (StrUtil.PATTERN_FIELD.matcher(name).matches()) {
                                    ann = ann.newFix(PascalActionDeclare.newActionCreateVar(message("action.create.field"), namedElement, adjustedScope, priority, null)).registerFix();
                                    if (context != PsiContext.PROPERTY_SPEC) {
                                        ann = ann.newFix(PascalActionDeclare.newActionCreateProperty(message("action.create.property"), namedElement, null, adjustedScope, false)).registerFix();
                                    }
                                } else {
                                    ann = ann.newFix(PascalActionDeclare.newActionCreateVar(message("action.create.field"), namedElement, adjustedScope, false, null)).registerFix();
                                    if (context != PsiContext.PROPERTY_SPEC) {
                                        ann = ann.newFix(PascalActionDeclare.newActionCreateProperty(message("action.create.property"), namedElement, null, adjustedScope, priority)).registerFix();
                                    }
                                }
                            }
                            break;
                        }
                        case TYPE: {
                            boolean priority = name.startsWith("T");
                            if (!(scope instanceof PascalStructType) || (context != PsiContext.FQN_NEXT)) {
                                ann = ann.newFix(PascalActionDeclare.newActionCreateType(namedElement, null, priority)).registerFix();
                                priority = false;             // lower priority for nested
                            }
                            ann = ann.newFix(PascalActionDeclare.newActionCreateType(namedElement, adjustScope(scope), priority)).registerFix();
                            break;
                        }
                        case CONST: {
                            boolean priority = !StrUtil.hasLowerCaseChar(name);
                            if ((scope instanceof PascalStructType)) {
                                ann = ann.newFix(PascalActionDeclare.newActionCreateConst(namedElement, null, priority)).registerFix();
                                priority = false;             // lower priority for nested
                            } else {
                                ann = ann.newFix(PascalActionDeclare.newActionCreateConst(namedElement, scope, priority)).registerFix();
                            }
                            ann = ann.newFix(PascalActionDeclare.newActionCreateConst(namedElement, adjustScope(scope), priority)).registerFix();
                            break;
                        }
                        case ROUTINE: {
                            boolean priority = context == PsiContext.CALL;
                            if (scope instanceof PascalStructType) {
                                if (context == PsiContext.PROPERTY_SPEC) {
                                    PasClassPropertySpecifier spec = PsiTreeUtil.getParentOfType(namedElement, PasClassPropertySpecifier.class);
                                    ann = ann.newFix(PascalActionDeclare.newActionCreateRoutine(message("action.create." + (ContextUtil.isPropertyGetter(spec) ? "getter" : "setter")),
                                            namedElement, scope, null, priority, spec)).registerFix();
                                } else {
                                    ann = ann.newFix(PascalActionDeclare.newActionCreateRoutine(message("action.create.method"), namedElement, scope, null, priority, null)).registerFix();
                                }
                            } else {
                                ann = ann.newFix(PascalActionDeclare.newActionCreateRoutine(message("action.create.routine"), namedElement, scope, null, priority, null)).registerFix();
                                PsiElement adjustedScope = adjustScope(scope);
                                if (adjustedScope instanceof PascalStructType) {
                                    ann = ann.newFix(PascalActionDeclare.newActionCreateRoutine(message("action.create.method"), namedElement, adjustedScope, scope, priority, null)).registerFix();
                                }
                            }
                            break;
                        }
                        case ENUM: {
                            ann = ann.newFix(new PascalActionDeclare.ActionCreateEnum(message("action.create.enumConst"), namedElement, scope)).registerFix();
                            break;
                        }
                        case PARAMETER: {
                            ann = ann.newFix(new PascalActionDeclare.ActionCreateParameter(namedElement, namedElement.getName(), scope)).registerFix();
                            break;
                        }
                        case UNIT: {
                            ann = ann.newFix(new UsesActions.AddUnitAction(message("action.add.uses", namedElement.getName()), namedElement.getName(), ContextUtil.belongsToInterface(namedElement))).registerFix();
                            break;
                        }
                        case NEW_UNIT: {
                            ann = ann.newFix(new UsesActions.NewUnitAction(message("action.create.unit"), namedElement.getName())).registerFix();
                            break;
                        }
                        case UNIT_FIND: {
                            ann = ann.newFix(new UsesActions.SearchUnitAction(namedElement, ContextUtil.belongsToInterface(namedElement))).registerFix();
                            break;
                        }
                    }
                }
                ann.create();
            }
        }
    }

    private void annotateModuleHead(PsiElement element, AnnotationHolder holder) {
        PasNamespaceIdent nameIdent = null;
        if (element instanceof PasUnitModuleHead) {
            nameIdent = ((PasUnitModuleHead) element).getNamespaceIdent();
        } else if (element instanceof PasLibraryModuleHead) {
            nameIdent = ((PasLibraryModuleHead) element).getNamespaceIdent();
        } else if (element instanceof PasProgramModuleHead) {
            nameIdent = ((PasProgramModuleHead) element).getNamespaceIdent();
        } else if (element instanceof PasPackageModuleHead) {
            nameIdent = ((PasPackageModuleHead) element).getNamespaceIdent();
        }
        if (nameIdent != null) {
            String fn = element.getContainingFile().getName();
            String fileName = FileUtil.getNameWithoutExtension(fn);
            if (!nameIdent.getName().equalsIgnoreCase(fileName)) {
                holder.newAnnotation(HighlightSeverity.ERROR, message("ann.error.unit.name.notmatch"))
                        .range(element)
                        .newFix(new PascalRenameAction(element, fileName, PascalBundle.message("action.module.rename"))).registerFix()
                        .newFix(new PascalRenameAction(element.getContainingFile(),
                                nameIdent.getName() + "." + FileUtilRt.getExtension(fn),
                                PascalBundle.message("action.file.rename"))).registerFix()
                        .create();
            }
        }
    }

    private boolean isVariantField(List<PsiElement> scopes) {
        return !scopes.isEmpty() && scopes.get(0) instanceof PasVariantScope;
    }

    private PsiElement adjustScope(PsiElement scope) {
        if (scope instanceof PascalRoutine) {
            PasEntityScope struct = ((PascalRoutine) scope).getContainingScope();
            if (struct instanceof PascalStructType) {
                return struct;
            }
        }
        return scope;
    }

    /**
     * # unimplemented routine error
     * # unimplemented method  error
     * # filter external/abstract routines/methods
     * # implement routine fix
     * # implement method fix
     * error on class if not all methods implemented
     * implement all methods fix
     */
    private void annotateRoutineInInterface(PasExportedRoutineImpl routine, AnnotationHolder holder) {
        if (!PsiUtil.isFromBuiltinsUnit(routine) && PsiUtil.needImplementation(routine) && (null == SectionToggle.retrieveImplementation(routine, true))) {
            holder.newAnnotation(HighlightSeverity.ERROR, message("ann.error.missing.implementation"))
                    .range(routine)
                    .newFix(new PascalRoutineActions.ActionImplement(message("action.implement"), routine)).registerFix()
                    .newFix(new PascalRoutineActions.ActionImplementAll(message("action.implement.all"), routine)).registerFix()
                    .create();
        }
    }

    /**
     * error on method in implementation only
     * add method to class declaration fix
     * add to interface section fix for routines in implementation section only
     */
    private void annotateRoutineInImplementation(PasRoutineImplDeclImpl routine, AnnotationHolder holder) {
        if (null == SectionToggle.retrieveDeclaration(routine, true)) {
            if (routine.getContainingScope() instanceof PasModule) {
                if (((PasModule) routine.getContainingScope()).getUnitInterface() != null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, message("ann.error.missing.routine.declaration"))
                            .range(routine.getNameIdentifier() != null ? routine.getNameIdentifier() : routine)
                            .newFix(new PascalRoutineActions.ActionDeclare(message("action.declare.routine"), routine)).registerFix()
                            .newFix(new PascalRoutineActions.ActionDeclareAll(message("action.declare.routine.all"), routine)).registerFix()
                            .create();
                }
            } else {
                holder.newAnnotation(HighlightSeverity.ERROR, message("ann.error.missing.method.declaration"))
                        .range(routine.getNameIdentifier() != null ? routine.getNameIdentifier() : routine)
                        .newFix(new PascalRoutineActions.ActionDeclare(message("action.declare.method"), routine)).registerFix()
                        .newFix(new PascalRoutineActions.ActionDeclareAll(message("action.declare.method.all"), routine)).registerFix()
                        .create();
            }
        }
    }

}
