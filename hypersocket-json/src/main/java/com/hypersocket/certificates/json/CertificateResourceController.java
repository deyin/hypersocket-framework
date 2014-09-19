package com.hypersocket.certificates.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.hypersocket.auth.json.AuthenticationRequired;
import com.hypersocket.auth.json.ResourceController;
import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.certificates.CertificateResource;
import com.hypersocket.certificates.CertificateResourceColumns;
import com.hypersocket.certificates.CertificateResourceService;
import com.hypersocket.certificates.CertificateResourceServiceImpl;
import com.hypersocket.certs.InvalidPassphraseException;
import com.hypersocket.certs.json.CertificateStatus;
import com.hypersocket.i18n.I18N;
import com.hypersocket.json.ResourceList;
import com.hypersocket.json.ResourceStatus;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.properties.json.PropertyItem;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.resource.ResourceException;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.resource.ResourceUpdate;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.tables.Column;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.tables.DataTablesResult;
import com.hypersocket.tables.json.DataTablesPageProcessor;

@Controller
public class CertificateResourceController extends ResourceController {

	/**
	 * TODO rename this class to match your entity.
	 * 
	 * rename RequestMapping annotions for your desired resource URLs. e.g
	 * replace certificates for example with "applications" certificates with "Applications"
	 * certificate with "application" and certificate with "Application"
	 */
	@Autowired
	CertificateResourceService resourceService;

	@AuthenticationRequired
	@RequestMapping(value = "certificates/table", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public DataTablesResult tableNetworkResources(
			final HttpServletRequest request, HttpServletResponse response)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			return processDataTablesRequest(request,
					new DataTablesPageProcessor() {

						@Override
						public Column getColumn(int col) {
							return CertificateResourceColumns.values()[col];
						}

						@Override
						public List<?> getPage(String searchPattern, int start,
								int length, ColumnSort[] sorting)
								throws UnauthorizedException,
								AccessDeniedException {
							return resourceService.searchResources(
									sessionUtils.getCurrentRealm(request),
									searchPattern, start, length, sorting);
						}

						@Override
						public Long getTotalCount(String searchPattern)
								throws UnauthorizedException,
								AccessDeniedException {
							return resourceService.getResourceCount(
									sessionUtils.getCurrentRealm(request),
									searchPattern);
						}
					});
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "certificates/template", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<PropertyCategory> getResourceTemplate(
			HttpServletRequest request) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			return new ResourceList<PropertyCategory>(resourceService.getPropertyTemplate());
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "certificates/properties/{id}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<PropertyCategory> getActionTemplate(
			HttpServletRequest request, @PathVariable Long id)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException, ResourceNotFoundException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			CertificateResource resource = resourceService.getResourceById(id);
			return new ResourceList<PropertyCategory>(resourceService.getPropertyTemplate(resource));
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "certificates/certificate/{id}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public CertificateResource getResource(HttpServletRequest request,
			HttpServletResponse response, @PathVariable("id") Long id)
			throws AccessDeniedException, UnauthorizedException,
			ResourceNotFoundException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			return resourceService.getResourceById(id);
		} finally {
			clearAuthenticatedContext();
		}

	}

	@AuthenticationRequired
	@RequestMapping(value = "certificates/certificate", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<CertificateResource> createOrUpdateNetworkResource(
			HttpServletRequest request, HttpServletResponse response,
			@RequestBody ResourceUpdate resource)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			CertificateResource newResource;

			Realm realm = sessionUtils.getCurrentRealm(request);

			Map<String, String> properties = new HashMap<String, String>();
			for (PropertyItem i : resource.getProperties()) {
				properties.put(i.getId(), i.getValue());
			}
			
			if (resource.getId() != null) {
				newResource = resourceService.updateResource(
						resourceService.getResourceById(resource.getId()),
						resource.getName(), properties);
			} else {
				newResource = resourceService.createResource(
						resource.getName(),
						realm,
						properties,
						false);
			}
			return new ResourceStatus<CertificateResource>(newResource,
					I18N.getResource(sessionUtils.getLocale(request),
							CertificateResourceServiceImpl.RESOURCE_BUNDLE,
							resource.getId() != null ? "resource.updated.info"
									: "resource.created.info", resource
									.getName()));

		} catch (ResourceChangeException e) {
			return new ResourceStatus<CertificateResource>(false,
					I18N.getResource(sessionUtils.getLocale(request),
							e.getBundle(), e.getResourceKey(), e.getArgs()));
		} catch (ResourceCreationException e) {
			return new ResourceStatus<CertificateResource>(false,
					I18N.getResource(sessionUtils.getLocale(request),
							e.getBundle(), e.getResourceKey(), e.getArgs()));
		} catch (ResourceNotFoundException e) {
			return new ResourceStatus<CertificateResource>(false,
					I18N.getResource(sessionUtils.getLocale(request),
							e.getBundle(), e.getResourceKey(), e.getArgs()));
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "certificates/certificate/{id}", method = RequestMethod.DELETE, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<CertificateResource> deleteResource(
			HttpServletRequest request, HttpServletResponse response,
			@PathVariable("id") Long id) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			CertificateResource resource = resourceService.getResourceById(id);

			if (resource == null) {
				return new ResourceStatus<CertificateResource>(false,
						I18N.getResource(sessionUtils.getLocale(request),
								CertificateResourceServiceImpl.RESOURCE_BUNDLE,
								"error.invalidResourceId", id));
			}

			String preDeletedName = resource.getName();
			resourceService.deleteResource(resource);

			return new ResourceStatus<CertificateResource>(true, I18N.getResource(
					sessionUtils.getLocale(request),
					CertificateResourceServiceImpl.RESOURCE_BUNDLE,
					"resource.deleted.info", preDeletedName));

		} catch (ResourceException e) {
			return new ResourceStatus<CertificateResource>(false, e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "certificates/downloadCSR/{id}", method = RequestMethod.GET, produces = { "text/plain" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public String generateCSR(HttpServletRequest request,
			HttpServletResponse response, @PathVariable Long id) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			CertificateStatus status = new CertificateStatus();
			status.setSuccess(false);
			try {
				
				CertificateResource resource = resourceService.getResourceById(id);
				String csr = resourceService.generateCSR(resource);
				response.setHeader("Content-Disposition", "attachment; filename=\"" + resource.getName() + ".csr\"");
				return csr;
			} catch (Exception e) {
				try {
					response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.ordinal());
				} catch (IOException e1) {
				}
				return null;
			}

		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "certificates/downloadCertificate/{id}", method = RequestMethod.GET, produces = { "text/plain" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public String downloadCertificate(HttpServletRequest request,
			HttpServletResponse response, @PathVariable Long id) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			CertificateStatus status = new CertificateStatus();
			status.setSuccess(false);
			try {
				
				CertificateResource resource = resourceService.getResourceById(id);
				String csr = resource.getCertificate();
				response.setHeader("Content-Disposition", "attachment; filename=\"" + resource.getName().replace(' ', '_') + ".crt\"");
				return csr;
			} catch (Exception e) {
				try {
					response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.ordinal());
				} catch (IOException e1) {
				}
				return null;
			}

		} finally {
			clearAuthenticatedContext();
		}
	}
	@AuthenticationRequired
	@RequestMapping(value = "certificates/cert/{id}", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public CertificateStatus uploadCertificate(HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable Long id,
			@RequestPart(value = "file") MultipartFile file,
			@RequestPart(value = "bundle") MultipartFile bundle)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			CertificateStatus status = new CertificateStatus();
			status.setSuccess(false);
			try {
				CertificateResource resource = resourceService.getResourceById(id);
				resourceService.updateCertificate(resource, file, bundle);
				status.setSuccess(true);
				status.setMessage(I18N.getResource(
						sessionUtils.getLocale(request),
						CertificateResourceServiceImpl.RESOURCE_BUNDLE, "info.certUploaded"));

			} catch (Exception ex) {
				status.setMessage(I18N.getResource(
						sessionUtils.getLocale(request),
						CertificateResourceServiceImpl.RESOURCE_BUNDLE,
						"error.generalError", ex.getMessage()));
			}

			return status;

		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "certificates/pem", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public CertificateStatus uploadKey(HttpServletRequest request,
			HttpServletResponse response,
			@RequestPart(value = "file") MultipartFile file,
			@RequestPart(value = "bundle") MultipartFile bundle,
			@RequestPart(value = "key") MultipartFile key,
			@RequestParam(value = "passphrase") String passphrase)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			CertificateStatus status = new CertificateStatus();
			status.setSuccess(false);
			try {
				resourceService.importPrivateKey(key, passphrase, file,
						bundle);
				status.setSuccess(true);
				status.setMessage(I18N.getResource(
						sessionUtils.getLocale(request),
						CertificateResourceServiceImpl.RESOURCE_BUNDLE, "info.keyUploaded"));
			} catch (InvalidPassphraseException e) {
				status.setMessage(I18N.getResource(
						sessionUtils.getLocale(request),
						CertificateResourceServiceImpl.RESOURCE_BUNDLE,
						"error.invalidPassphrase"));
			} catch (Exception e) {
				status.setMessage(I18N.getResource(
						sessionUtils.getLocale(request),
						CertificateResourceServiceImpl.RESOURCE_BUNDLE,
						"error.generalError", e.getMessage()));
			}

			return status;

		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "certificates/pfx", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public CertificateStatus uploadPfx(HttpServletRequest request,
			HttpServletResponse response,
			@RequestPart(value = "key") MultipartFile key,
			@RequestParam(value = "passphrase") String passphrase)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			CertificateStatus status = new CertificateStatus();
			status.setSuccess(false);
			try {
				resourceService.importPfx(key, passphrase);
				status.setSuccess(true);
				status.setMessage(I18N.getResource(
						sessionUtils.getLocale(request),
						CertificateResourceServiceImpl.RESOURCE_BUNDLE, "info.keyUploaded"));
//			} catch (InvalidPassphraseException e) {
//				status.setMessage(I18N.getResource(
//						sessionUtils.getLocale(request),
//						CertificateService.RESOURCE_BUNDLE,
//						"error.invalidPassphrase"));
			} catch (Exception e) {
				status.setMessage(I18N.getResource(
						sessionUtils.getLocale(request),
						CertificateResourceServiceImpl.RESOURCE_BUNDLE,
						"error.generalError", e.getMessage()));
			}

			return status;

		} finally {
			clearAuthenticatedContext();
		}
	}
}
