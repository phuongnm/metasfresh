package de.metas.ui.web.window.descriptor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import org.adempiere.ad.expression.api.ILogicExpression;
import org.adempiere.ad.expression.api.IStringExpression;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DisplayType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.datatypes.json.JSONDate;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;
import de.metas.ui.web.window.descriptor.DocumentFieldDependencyMap.DependencyType;
import de.metas.ui.web.window.model.LookupDataSource;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@SuppressWarnings("serial")
public final class DocumentFieldDescriptor implements Serializable
{
	public static final Builder builder()
	{
		return new Builder();
	}

	/** Internal field name (aka ColumnName) */
	@JsonProperty("fieldName")
	private final String fieldName;
	/** Detail ID or null if this is a field in main sections */
	@JsonProperty("detailId")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String detailId;

	/** Is this the key field ? */
	@JsonProperty("key")
	private final boolean key;
	@JsonProperty("parentLink")
	private final boolean parentLink;
	@JsonProperty("virtualField")
	private final boolean virtualField;
	@JsonProperty("calculated")
	private final boolean calculated;

	@JsonProperty("widgetType")
	private final DocumentFieldWidgetType widgetType;

	@JsonProperty("valueClass")
	private final Class<?> valueClass;

	@JsonProperty("defaultValueExpression")
	private final IStringExpression defaultValueExpression;

	@JsonProperty("public")
	private final boolean publicField;
	@JsonProperty("advanced")
	private final boolean advancedField;
	@JsonProperty("side-list")
	private final boolean sideListField;
	@JsonProperty("readonlyLogic")
	private final ILogicExpression readonlyLogic;
	@JsonProperty("alwaysUpdateable")
	private final boolean alwaysUpdateable;
	@JsonProperty("displayLogic")
	private final ILogicExpression displayLogic;
	@JsonProperty("mandatoryLogic")
	private final ILogicExpression mandatoryLogic;

	@JsonProperty("data-binding")
	private final DocumentFieldDataBindingDescriptor dataBinding;

	@JsonIgnore
	private final DocumentFieldDependencyMap dependencies;

	private DocumentFieldDescriptor(final Builder builder)
	{
		super();
		fieldName = Preconditions.checkNotNull(builder.fieldName, "name is null");
		detailId = builder.detailId;

		key = builder.key;
		parentLink = builder.parentLink;
		virtualField = builder.virtualField;
		calculated = builder.calculated;

		widgetType = Preconditions.checkNotNull(builder.widgetType, "widgetType is null");

		valueClass = Preconditions.checkNotNull(builder.valueClass, "value class not null");

		defaultValueExpression = builder.defaultValueExpression;

		publicField = builder.publicField;
		advancedField = builder.advancedField;
		sideListField = builder.sideListField;
		readonlyLogic = builder.readonlyLogic;
		alwaysUpdateable = builder.alwaysUpdateable;
		displayLogic = builder.displayLogic;
		mandatoryLogic = builder.mandatoryLogic;

		dataBinding = builder.getDataBinding();

		dependencies = builder.buildDependencies();
	}

	@JsonCreator
	private DocumentFieldDescriptor(
			@JsonProperty("fieldName") final String fieldName //
			, @JsonProperty("detailId") final String detailId //
			, @JsonProperty("key") final boolean key //
			, @JsonProperty("parentLink") final boolean parentLink //
			, @JsonProperty("virtualField") final boolean virtualField //
			, @JsonProperty("calculated") final boolean calculated //
			, @JsonProperty("widgetType") final DocumentFieldWidgetType widgetType //
			, @JsonProperty("valueClass") final Class<?> valueClass //
			, @JsonProperty("defaultValueExpression") final IStringExpression defaultValueExpression //
			, @JsonProperty("public") final boolean publicField //
			, @JsonProperty("advanced") final boolean advancedField //
			, @JsonProperty("readonlyLogic") final ILogicExpression readonlyLogic //
			, @JsonProperty("alwaysUpdateable") final boolean alwaysUpdateable //
			, @JsonProperty("displayLogic") final ILogicExpression displayLogic //
			, @JsonProperty("mandatoryLogic") final ILogicExpression mandatoryLogic //
			, @JsonProperty("data-binding") final DocumentFieldDataBindingDescriptor dataBinding //
	)
	{
		this(new Builder()
				.setFieldName(fieldName)
				.setDetailId(detailId)
				.setKey(key)
				.setParentLink(parentLink)
				.setVirtualField(virtualField)
				.setCalculated(calculated)
				.setWidgetType(widgetType)
				.setValueClass(valueClass)
				.setDefaultValueExpression(defaultValueExpression)
				.setPublicField(publicField)
				.setAdvancedField(advancedField)
				.setReadonlyLogic(readonlyLogic)
				.setAlwaysUpdateable(alwaysUpdateable)
				.setDisplayLogic(displayLogic)
				.setMandatoryLogic(mandatoryLogic)
				.setDataBinding(dataBinding));
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("name", fieldName)
				.add("detailId", detailId)
				.add("widgetType", widgetType)
				.add("publicField", publicField)
				.add("sideListField", sideListField ? Boolean.TRUE : null)
				.add("fieldDataBinding", dataBinding)
				.toString();
	}

	public String getFieldName()
	{
		return fieldName;
	}

	public String getDetailId()
	{
		return detailId;
	}

	public boolean isKey()
	{
		return key;
	}

	public boolean isParentLink()
	{
		return parentLink;
	}

	public boolean isVirtualField()
	{
		return virtualField;
	}

	public boolean isCalculated()
	{
		return calculated;
	}

	public DocumentFieldWidgetType getWidgetType()
	{
		return widgetType;
	}

	public Class<?> getValueClass()
	{
		return valueClass;
	}

	public IStringExpression getDefaultValueExpression()
	{
		return defaultValueExpression;
	}

	/**
	 * @return true if this field is public and will be published to API clients
	 */
	public boolean isPublicField()
	{
		return publicField;
	}

	public boolean isAdvancedField()
	{
		return advancedField;
	}

	public boolean isSideListField()
	{
		return sideListField;
	}

	public ILogicExpression getReadonlyLogic()
	{
		return readonlyLogic;
	}

	public boolean isAlwaysUpdateable()
	{
		return alwaysUpdateable;
	}

	public ILogicExpression getDisplayLogic()
	{
		return displayLogic;
	}

	public ILogicExpression getMandatoryLogic()
	{
		return mandatoryLogic;
	}

	/**
	 * @return field data binding info; never null
	 */
	public DocumentFieldDataBindingDescriptor getDataBinding()
	{
		return dataBinding;
	}

	public DocumentFieldDependencyMap getDependencies()
	{
		return dependencies;
	}

	public <T> T convertToValueClass(final Object value, final Class<T> targetType, final LookupDataSource lookupDataSource)
	{
		if (value == null)
		{
			return null;
		}

		final Class<?> fromType = value.getClass();

		try
		{
			// Corner case: we need to convert Timestamp(which extends Date) to strict Date because else all value changed comparing methods will fail
			if (java.util.Date.class.equals(targetType) && Timestamp.class.equals(fromType))
			{
				@SuppressWarnings("unchecked")
				final T valueConv = (T)JSONDate.fromTimestamp((Timestamp)value);
				return valueConv;
			}

			if (targetType.isAssignableFrom(fromType))
			{
				@SuppressWarnings("unchecked")
				final T valueConv = (T)value;
				return valueConv;
			}

			if (String.class == targetType)
			{
				if (Map.class.isAssignableFrom(fromType))
				{
					// this is not allowed for consistency. let it fail.
				}
				// For any other case, blindly convert it to string
				else
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)value.toString();
					return valueConv;
				}
			}
			else if (java.util.Date.class == targetType)
			{
				if (value instanceof String)
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)JSONDate.fromJson((String)value);
					return valueConv;
				}
			}
			else if (Integer.class == targetType)
			{
				if (value instanceof String)
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(Integer)Integer.parseInt((String)value);
					return valueConv;
				}
				else if (value instanceof Number)
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(Integer)((Number)value).intValue();
					return valueConv;
				}
			}
			else if (BigDecimal.class == targetType)
			{
				if (String.class == fromType)
				{
					final String valueStr = (String)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(valueStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(valueStr));
					return valueConv;
				}
				else if (Integer.class == fromType || int.class == fromType)
				{
					final int valueInt = (int)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)BigDecimal.valueOf(valueInt);
					return valueConv;
				}
			}
			else if (Boolean.class == targetType)
			{
				@SuppressWarnings("unchecked")
				final T valueConv = (T)DisplayType.toBoolean(value, Boolean.FALSE);
				return valueConv;
			}
			else if (IntegerLookupValue.class == targetType)
			{
				if (Map.class.isAssignableFrom(fromType))
				{
					@SuppressWarnings("unchecked")
					final Map<String, String> map = (Map<String, String>)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)JSONLookupValue.integerLookupValueFromJsonMap(map);
					return valueConv;
				}
				else if (Integer.class.isAssignableFrom(fromType))
				{
					final Integer valueInt = (Integer)value;
					if (lookupDataSource != null)
					{
						final LookupValue valueLookup = lookupDataSource.findById(valueInt);
						final T valueConv = convertToValueClass(valueLookup, targetType, /* lookupDataSource */null);
						// TODO: what if valueConv was not found?
						return valueConv;
					}
				}
				else if (String.class == fromType)
				{
					final String valueStr = (String)value;
					if (valueStr.isEmpty())
					{
						return null;
					}

					if (lookupDataSource != null)
					{
						final LookupValue valueLookup = lookupDataSource.findById(valueStr);
						final T valueConv = convertToValueClass(valueLookup, targetType, /* lookupDataSource */null);
						// TODO: what if valueConv was not found?
						return valueConv;
					}
				}
			}
			else if (StringLookupValue.class == targetType)
			{
				if (Map.class.isAssignableFrom(fromType))
				{
					@SuppressWarnings("unchecked")
					final Map<String, String> map = (Map<String, String>)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)JSONLookupValue.stringLookupValueFromJsonMap(map);
					return valueConv;
				}
				else if (String.class == fromType)
				{
					final String valueStr = (String)value;
					if (valueStr.isEmpty())
					{
						return null;
					}

					if (lookupDataSource != null)
					{
						final LookupValue valueLookup = lookupDataSource.findById(valueStr);
						final T valueConv = convertToValueClass(valueLookup, targetType, /* lookupDataSource */null);
						// TODO: what if valueConv was not found?
						return valueConv;
					}
				}
				else if (IntegerLookupValue.class == fromType)
				{
					final IntegerLookupValue lookupValueInt = (IntegerLookupValue)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)StringLookupValue.of(lookupValueInt.getIdAsString(), lookupValueInt.getDisplayName());
					return valueConv;
				}
			}
		}
		catch (final Exception e)
		{
			throw new AdempiereException("Cannot convert " + fieldName + "'s value '" + value + "' (" + fromType + ") to " + targetType, e);
		}

		throw new AdempiereException("Cannot convert " + fieldName + "'s value '" + value + "' (" + fromType + ") to " + targetType);
	}

	public static final class Builder
	{

		private DocumentFieldDescriptor _fieldBuilt;

		private String fieldName;
		public String detailId;

		private boolean key = false;
		private boolean parentLink = false;
		private boolean virtualField;
		private boolean calculated;

		private DocumentFieldWidgetType widgetType;
		public Class<?> valueClass;

		private IStringExpression defaultValueExpression = IStringExpression.NULL;

		private Boolean publicField;
		private Boolean advancedField;
		private boolean sideListField = false;
		private ILogicExpression readonlyLogic = ILogicExpression.FALSE;
		private boolean alwaysUpdateable;
		private ILogicExpression displayLogic = ILogicExpression.TRUE;
		private ILogicExpression mandatoryLogic = ILogicExpression.FALSE;

		private DocumentFieldDataBindingDescriptor _dataBinding;

		private Builder()
		{
			super();
		}

		public DocumentFieldDescriptor getOrBuild()
		{
			if (_fieldBuilt == null)
			{
				_fieldBuilt = new DocumentFieldDescriptor(this);
			}
			return _fieldBuilt;
		}

		private final void assertNotBuilt()
		{
			if (_fieldBuilt != null)
			{
				throw new IllegalStateException("Already built: " + this);
			}
		}

		public Builder setFieldName(final String fieldName)
		{
			assertNotBuilt();
			this.fieldName = fieldName;
			return this;
		}

		public Builder setDetailId(final String detailId)
		{
			assertNotBuilt();
			this.detailId = Strings.emptyToNull(detailId);
			return this;
		}

		public Builder setKey(final boolean key)
		{
			assertNotBuilt();
			this.key = key;
			return this;
		}

		public Builder setParentLink(final boolean parentLink)
		{
			assertNotBuilt();
			this.parentLink = parentLink;
			return this;
		}

		public Builder setVirtualField(final boolean virtualField)
		{
			assertNotBuilt();
			this.virtualField = virtualField;
			return this;
		}

		public Builder setCalculated(final boolean calculated)
		{
			assertNotBuilt();
			this.calculated = calculated;
			return this;
		}

		public Builder setWidgetType(final DocumentFieldWidgetType widgetType)
		{
			assertNotBuilt();
			this.widgetType = widgetType;
			return this;
		}

		public Builder setValueClass(final Class<?> valueClass)
		{
			assertNotBuilt();
			this.valueClass = valueClass;
			return this;
		}

		public Builder setDefaultValueExpression(final IStringExpression defaultValueExpression)
		{
			assertNotBuilt();
			this.defaultValueExpression = Preconditions.checkNotNull(defaultValueExpression);
			return this;
		}

		/**
		 * @param publicField true if this field is public and will be published to API clients
		 */
		public Builder setPublicField(final boolean publicField)
		{
			assertNotBuilt();
			this.publicField = publicField;
			return this;
		}

		public Builder setAdvancedField(final boolean advancedField)
		{
			assertNotBuilt();
			this.advancedField = advancedField;
			return this;
		}

		public Builder setSideListField(final boolean sideListField)
		{
			assertNotBuilt();
			this.sideListField = sideListField;
			return this;
		}

		public Builder setReadonlyLogic(final ILogicExpression readonlyLogic)
		{
			assertNotBuilt();
			this.readonlyLogic = Preconditions.checkNotNull(readonlyLogic);
			return this;
		}

		public Builder setAlwaysUpdateable(final boolean alwaysUpdateable)
		{
			assertNotBuilt();
			this.alwaysUpdateable = alwaysUpdateable;
			return this;
		}

		public Builder setDisplayLogic(final ILogicExpression displayLogic)
		{
			assertNotBuilt();
			this.displayLogic = Preconditions.checkNotNull(displayLogic);
			return this;
		}

		public Builder setMandatoryLogic(final ILogicExpression mandatoryLogic)
		{
			assertNotBuilt();
			this.mandatoryLogic = Preconditions.checkNotNull(mandatoryLogic);
			return this;
		}

		public Builder setDataBinding(final DocumentFieldDataBindingDescriptor dataBinding)
		{
			assertNotBuilt();
			_dataBinding = dataBinding;
			return this;
		}

		public DocumentFieldDataBindingDescriptor getDataBinding()
		{
			Preconditions.checkNotNull(_dataBinding, "dataBinding is null");
			return _dataBinding;
		}

		private DocumentFieldDependencyMap buildDependencies()
		{
			return DocumentFieldDependencyMap.builder()
					.add(fieldName, readonlyLogic.getParameters(), DependencyType.ReadonlyLogic)
					.add(fieldName, displayLogic.getParameters(), DependencyType.DisplayLogic)
					.add(fieldName, mandatoryLogic.getParameters(), DependencyType.MandatoryLogic)
					.add(fieldName, getDataBinding().getLookupValuesDependsOnFieldNames(), DependencyType.LookupValues)
					.build();
		}

	}
}
