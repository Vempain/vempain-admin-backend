package fi.poltsi.vempain.admin.api;

public enum QueryDetailEnum {
	MINIMAL, // This means that there will only be the id and then a field suitable as a label
	UNPOPULATED, // This means that only the entity data is returned, no related entities are populated
	FULL
}
