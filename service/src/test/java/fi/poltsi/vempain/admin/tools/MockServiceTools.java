package fi.poltsi.vempain.admin.tools;

import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.exception.EntityAlreadyExistsException;
import fi.poltsi.vempain.admin.exception.InvalidRequestException;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.service.AclService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MockServiceTools {
    ////////// Acl service start
    public static void aclServiceFindAllOk(AclService aclService, long count) {
        ArrayList<Acl> acls = new ArrayList<>();

        for (long i = 0; i < count; i++) {
            acls.addAll(getOneOfEachAcl(i));
        }

        when(aclService.findAll()).thenReturn(acls);
    }

    public static void aclServicefindAclByAclIdOk(AclService aclService, long aclId) {
        List<Acl> acls = getOneOfEachAcl(aclId);

        when(aclService.findAclByAclId(aclId)).thenReturn(acls);
    }


    public static void aclServicefindAclByAclIdEmptyList(AclService aclService, long aclId) {
        List<Acl> acls = new ArrayList<>();

        when(aclService.findAclByAclId(aclId)).thenReturn(acls);
    }

    ////////// Acl service private methods

    private static List<Acl> getOneOfEachAcl(long aclId) {
        ArrayList<Acl> acls = new ArrayList<>();

        for (long i = 1; i < 5; i++) {
            acls.add(Acl.builder()
                                .aclId(aclId)
                                .userId(i)
                                .unitId(null)
                                .readPrivilege((i == 1))
                                .modifyPrivilege((i == 2))
                                .createPrivilege((i == 3))
                                .deletePrivilege((i == 4))
                                .build());
            acls.add(Acl.builder()
                                .aclId(aclId)
                                .userId(null)
                                .unitId(1L)
                                .readPrivilege((i == 1))
                                .modifyPrivilege((i == 2))
                                .createPrivilege((i == 3))
                                .deletePrivilege((i == 4))
                                .build());
        }

        return acls;
    }

    private static List<AclResponse> getOneOfEachAclResponse(long aclId) {
        ArrayList<AclResponse> acls = new ArrayList<>();

        for (long i = 1; i < 5; i++) {
            acls.add(AclResponse.builder()
                                .aclId(aclId)
                                .user(i)
                                .unit(null)
                                .readPrivilege((i == 1))
                                .modifyPrivilege((i == 2))
                                .createPrivilege((i == 3))
                                .deletePrivilege((i == 4))
                                .build());
            acls.add(AclResponse.builder()
                                .aclId(aclId)
                                .user(null)
                                .unit(1L)
                                .readPrivilege((i == 1))
                                .modifyPrivilege((i == 2))
                                .createPrivilege((i == 3))
                                .deletePrivilege((i == 4))
                                .build());
        }

        return acls;
    }

    ////////// Acl service end

    ////////// Component service start
    ////////// Component service private methods
    private static List<Component> getListOfComponents(long count) {
        ArrayList<Component> components = new ArrayList<>();

        for (long i = 0; i < count; i++) {
            components.add(Component.builder()
                                    .id(i + 1)
                                    .compName("Test component " + i)
                                    .compData("Component data " + i)
                                    .locked(false)
                                    .aclId(1L)
                                    .creator(1L)
                                    .created(Instant.now().minus(1, ChronoUnit.HOURS))
                                    .modifier(1L)
                                    .modified(Instant.now())
                                    .build());
        }

        return components;
    }

    private static List<ComponentResponse> getListOfComponentResponses(long count) {
        List<Component> components = getListOfComponents(count);
        ArrayList<ComponentResponse> responses = new ArrayList<>();

        for (Component component : components) {
            ComponentResponse componentResponse = component.getComponentResponse();
            componentResponse.setAcls(getOneOfEachAclResponse(3L));
            responses.add(componentResponse);
        }

        return responses;
    }

    ////////// Component service end

    ////////// Form service start
    public static void formServiceFindAllOk(FormService formService, long count) {
        List<Form> forms = getListOfForms(count);
        when(formService.findAll()).thenReturn(forms);
    }

    public static void formServiceFindAllAsResponsesForUserOk(FormService formService, long count) throws VempainComponentException {
        List<Form> forms = getListOfForms(count);
        ArrayList<FormResponse> formResponses = new ArrayList<>();

        for (Form form : forms) {
            FormResponse formResponse = form.getFormResponse();
            formResponse.setAcls(getOneOfEachAclResponse(1L));
            formResponse.setComponents(getListOfComponentResponses(4L));
            formResponses.add(formResponse);
        }

        when(formService.findAllAsResponsesForUser(any(QueryDetailEnum.class))).thenReturn(formResponses);
    }

    public static void formServiceAddFormOk(FormService formService) throws EntityAlreadyExistsException, InvalidRequestException, ProcessingFailedException {
        List<Form> aForm = getListOfForms(1);
        FormResponse formResponse = aForm.getFirst().getFormResponse();
        formResponse.setAcls(getOneOfEachAclResponse(1L));
        formResponse.setComponents(getListOfComponentResponses(4L));
        when(formService.saveRequest(any())).thenReturn(formResponse);
    }

    ////////// Form service private methods

    private static List<Form> getListOfForms(long count) {
        ArrayList<Form> forms = new ArrayList<>();

        for (long i = 0; i < count; i++) {
            forms.add(Form.builder()
                          .id(i + 1)
                          .formName("Test form " + i)
                          .layoutId(i + 1)
                          .aclId(i + 1)
                          .locked(false)
                          .creator(i + 1)
                          .created(Instant.now().minus(1, ChronoUnit.HOURS))
                          .modifier(i + 1)
                          .modified(Instant.now())
                          .build());
        }

        return forms;
    }

    ////////// Form service end
    ////////// Layout service start
    ////////// Layout service private methods
    ////////// Layout service end
}
