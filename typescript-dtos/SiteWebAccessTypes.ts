/**
 * TypeScript type definitions for Site Web Access Management
 */
export type WebSiteResourceType = 'SITE_FILE' | 'PAGE' | 'GALLERY';

export interface WebSiteResourceResponse {
	resource_type: WebSiteResourceType;
	resource_id: number;
	name: string;
	path?: string;
	acl_id: number;
	file_type?: string;
}

export interface WebSiteResourcePageResponse {
	page_number: number;
	page_size: number;
	total_pages: number;
	total_elements: number;
	items: WebSiteResourceResponse[];
}

export interface WebSiteResourceQueryParams {
	type?: WebSiteResourceType;
	file_type?: string; // shortName from FileTypeEnum
	query?: string;
	acl_id?: number;
	sort?: string; // id | name | path | title | shortname | description | acl_id | file_type | created
	direction?: 'asc' | 'desc';
	page?: number;
	size?: number;
}

export interface WebSiteResourceServiceApi {
	getResources(params: WebSiteResourceQueryParams): Promise<WebSiteResourcePageResponse>;
}

export function buildResourceQuery(params: WebSiteResourceQueryParams): string {
	const q = new URLSearchParams();
	if (params.type) q.set('type', params.type);
	if (params.file_type) q.set('file_type', params.file_type);
	if (params.query) q.set('query', params.query);
	if (params.acl_id != null) q.set('acl_id', String(params.acl_id));
	if (params.sort) q.set('sort', params.sort);
	if (params.direction) q.set('direction', params.direction);
	if (params.page != null) q.set('page', String(params.page));
	if (params.size != null) q.set('size', String(params.size));
	return q.toString();
}

export function isWebSiteResourceResponse(value: unknown): value is WebSiteResourceResponse {
	if (typeof value !== 'object' || value === null) return false;
	const v = value as WebSiteResourceResponse;
	return typeof v.resource_type === 'string' && typeof v.resource_id === 'number' && typeof v.acl_id === 'number';
}
