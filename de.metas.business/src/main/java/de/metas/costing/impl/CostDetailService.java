package de.metas.costing.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.trx.processor.api.FailTrxItemExceptionHandler;
import org.adempiere.ad.trx.processor.api.ITrxItemProcessorExecutorService;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.service.IClientDAO;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_AD_Client;
import org.compiere.model.I_C_AcctSchema;
import org.compiere.model.I_M_CostDetail;
import org.compiere.model.I_M_Product;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MProduct;
import org.compiere.model.X_M_CostElement;
import org.compiere.util.Env;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import de.metas.costing.CostDetailCreateRequest;
import de.metas.costing.CostDetailEvent;
import de.metas.costing.CostDetailQuery;
import de.metas.costing.CostElement;
import de.metas.costing.CostSegment;
import de.metas.costing.CostingDocumentRef;
import de.metas.costing.CostingLevel;
import de.metas.costing.CostingMethodHandler;
import de.metas.costing.ICostDetailRepository;
import de.metas.costing.ICostDetailService;
import de.metas.costing.ICostElementRepository;
import de.metas.costing.methods.AverageInvoiceCostingMethodHandler;
import de.metas.costing.methods.AveragePOCostingMethodHandler;
import de.metas.costing.methods.FifoCostingMethodHandler;
import de.metas.costing.methods.LastInvoiceCostingMethodHandler;
import de.metas.costing.methods.LastPOCostingMethodHandler;
import de.metas.costing.methods.LifoCostingMethodHandler;
import de.metas.costing.methods.StandardCostingMethodHandler;
import de.metas.logging.LogManager;
import de.metas.product.IProductBL;
import lombok.NonNull;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class CostDetailService implements ICostDetailService
{
	private static final Logger logger = LogManager.getLogger(CostDetailService.class);

	private final ICostDetailRepository costDetailsRepo = Services.get(ICostDetailRepository.class);

	private final ImmutableMap<String, CostingMethodHandler> costingMethodHandlers = ImmutableMap.<String, CostingMethodHandler> builder()
			.put(X_M_CostElement.COSTINGMETHOD_AveragePO, new AveragePOCostingMethodHandler())
			.put(X_M_CostElement.COSTINGMETHOD_LastPOPrice, new LastPOCostingMethodHandler())
			.put(X_M_CostElement.COSTINGMETHOD_AverageInvoice, new AverageInvoiceCostingMethodHandler())
			.put(X_M_CostElement.COSTINGMETHOD_LastInvoice, new LastInvoiceCostingMethodHandler())
			.put(X_M_CostElement.COSTINGMETHOD_Fifo, new FifoCostingMethodHandler())
			.put(X_M_CostElement.COSTINGMETHOD_Lifo, new LifoCostingMethodHandler())
			.put(X_M_CostElement.COSTINGMETHOD_StandardCosting, new StandardCostingMethodHandler())
			.build();

	@Override
	public void createCostDetail(@NonNull final CostDetailCreateRequest request)
	{
		extractCostElements(request)
				.forEach(costElement -> {
					final CostingMethodHandler costingMethodHandler = getCostingMethodHandler(costElement.getCostingMethod());

					final I_M_CostDetail costDetail = costingMethodHandler.createCost(request.toBuilder()
							.costElementId(costElement.getId())
							.build());
					if (costDetail == null)
					{
						return;
					}

					processIfCostImmediate(costDetail);
				});
	}

	@Override
	public void processIfCostImmediate(final I_M_CostDetail costDetail)
	{
		if (costDetail.isProcessed())
		{
			return;
		}

		final I_AD_Client client = Services.get(IClientDAO.class).retriveClient(Env.getCtx(), costDetail.getAD_Client_ID());
		if (!client.isCostImmediate())
		{
			return;
		}

		process(costDetail);
	}

	@Override
	public void processAllForProduct(final int productId)
	{
		final Iterator<I_M_CostDetail> costDetails = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_CostDetail.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_CostDetail.COLUMN_M_Product_ID, productId)
				.addEqualsFilter(I_M_CostDetail.COLUMN_Processed, false)
				.orderBy(I_M_CostDetail.COLUMN_C_AcctSchema_ID)
				.orderBy(I_M_CostDetail.COLUMN_M_CostElement_ID)
				.orderBy(I_M_CostDetail.COLUMN_AD_Org_ID)
				.orderBy(I_M_CostDetail.COLUMN_M_AttributeSetInstance_ID)
				.orderBy(I_M_CostDetail.COLUMN_Created)
				.create()
				.iterate(I_M_CostDetail.class);
		if (!costDetails.hasNext())
		{
			return;
		}

		Services.get(ITrxItemProcessorExecutorService.class)
				.<I_M_CostDetail, Void> createExecutor()
				.setExceptionHandler(FailTrxItemExceptionHandler.instance)
				.setProcessor(this::process)
				.process(costDetails);
	}

	@Override
	public void reverseAndDeleteForDocument(final CostingDocumentRef documentRef)
	{
		costDetailsRepo.getAllForDocument(documentRef)
				.forEach(this::reverseAndDelete);
	}

	private void reverseAndDelete(final I_M_CostDetail costDetail)
	{
		if (costDetail.isProcessed())
		{
			costDetail.setProcessed(false);
			costDetail.setDeltaAmt(costDetail.getAmt());
			costDetail.setDeltaQty(costDetail.getQty());
			process(costDetail);
			Check.assume(costDetail.isProcessed(), "Cost detail {} shall be processed at this point", costDetail); // shall not happen
		}

		costDetailsRepo.delete(costDetail);
	}

	@Override
	public void reversePartialQty(@NonNull final CostDetailQuery query, @NonNull final BigDecimal qty)
	{
		final I_M_CostDetail costDetail = costDetailsRepo.getCostDetailOrNull(query);
		if (costDetail == null)
		{
			return;
		}

		final BigDecimal price;
		if (costDetail.getQty().signum() == 0)
		{
			price = costDetail.getAmt(); // FIXME: shall use price=0?
		}
		else
		{
			price = costDetail.getAmt().divide(costDetail.getQty(), 12, BigDecimal.ROUND_HALF_UP);
		}
		costDetail.setDeltaAmt(price.multiply(qty.negate()));
		costDetail.setDeltaQty(qty.negate());
		costDetail.setProcessed(false);

		costDetail.setAmt(price.multiply(costDetail.getQty().subtract(qty)));
		costDetail.setQty(costDetail.getQty().subtract(qty));
		processIfCostImmediate(costDetail);

		if (costDetail.getQty().signum() == 0)
		{
			costDetailsRepo.delete(costDetail);
		}
	}

	private void process(final I_M_CostDetail costDetail)
	{
		if (costDetail.isProcessed())
		{
			logger.debug("Cost detail already processed: {}", costDetail);
			return;
		}

		final CostSegment costSegment = extractCostSegment(costDetail);
		extractCostElements(costDetail)
				.stream()
				.map(costElement -> createCostDetailEvent(costDetail, costSegment, costElement))
				.forEach(this::process);

		// Save it
		costDetail.setDeltaAmt(null);
		costDetail.setDeltaQty(null);
		costDetail.setProcessed(true);
		costDetailsRepo.save(costDetail);

		logger.debug("processed: {}", costDetail);
	}

	private List<CostElement> extractCostElements(final I_M_CostDetail cd)
	{
		final ICostElementRepository costElementRepository = Services.get(ICostElementRepository.class);

		final int costElementId = cd.getM_CostElement_ID();
		if (costElementId <= 0)
		{
			return costElementRepository.getMaterialCostingMethods(cd.getAD_Client_ID());
		}
		else
		{
			final CostElement ce = costElementRepository.getById(costElementId);
			return ImmutableList.of(ce);
		}
	}

	private List<CostElement> extractCostElements(final CostDetailCreateRequest request)
	{
		final ICostElementRepository costElementRepository = Services.get(ICostElementRepository.class);

		final int costElementId = request.getCostElementId();
		if (costElementId <= 0)
		{
			return costElementRepository.getMaterialCostingMethods(request.getClientId());
		}
		else
		{
			final CostElement ce = costElementRepository.getById(costElementId);
			return ImmutableList.of(ce);
		}
	}

	private CostSegment extractCostSegment(final I_M_CostDetail cd)
	{
		final int productId = cd.getM_Product_ID();
		final int acctSchemaId = cd.getC_AcctSchema_ID();

		// get costing level for product
		final Properties ctx = Env.getCtx();
		final I_C_AcctSchema acctSchema = MAcctSchema.get(ctx, acctSchemaId);
		final I_M_Product product = MProduct.get(ctx, productId);
		final CostingLevel costingLevel = CostingLevel.forCode(Services.get(IProductBL.class).getCostingLevel(product, acctSchema));

		final int orgId;
		final int attributeSetInstanceId;
		if (costingLevel == CostingLevel.Client)
		{
			orgId = 0;
			attributeSetInstanceId = 0;
		}
		else if (costingLevel == CostingLevel.Organization)
		{
			orgId = cd.getAD_Org_ID();
			attributeSetInstanceId = 0;
		}
		else if (costingLevel == CostingLevel.BatchLot)
		{
			orgId = 0;
			attributeSetInstanceId = cd.getM_AttributeSetInstance_ID();
		}
		else
		{
			throw new AdempiereException("Unknown costing level: " + costingLevel);
		}

		return CostSegment.builder()
				.acctSchemaId(acctSchemaId)
				.costTypeId(acctSchema.getM_CostType_ID())
				.costingLevel(costingLevel)
				.productId(productId)
				.clientId(cd.getAD_Client_ID())
				.orgId(orgId)
				.attributeSetInstanceId(attributeSetInstanceId)
				.build();
	}

	@Override
	public void onCostDetailDeleted(final I_M_CostDetail costDetail)
	{
		final CostSegment costSegment = extractCostSegment(costDetail);
		extractCostElements(costDetail)
				.stream()
				.map(costElement -> createCostDetailEvent(costDetail, costSegment, costElement))
				.forEach(this::process);
	}

	private CostingMethodHandler getCostingMethodHandler(final String costingMethod)
	{
		final CostingMethodHandler costingMethodHandler = costingMethodHandlers.get(costingMethod);
		if (costingMethodHandler == null)
		{
			throw new AdempiereException("No " + CostingMethodHandler.class + " foudn for " + costingMethod);
		}
		return costingMethodHandler;
	}

	private CostingDocumentRef extractDocumentRef(final I_M_CostDetail cd)
	{
		if (cd.getC_OrderLine_ID() > 0)
		{
			if (cd.isSOTrx())
			{
				throw new AdempiereException("Sales order line not supported: " + cd);
			}
			return CostingDocumentRef.ofPurchaseOrderLineId(cd.getC_OrderLine_ID());
		}
		else if (cd.getC_InvoiceLine_ID() > 0)
		{
			if (cd.isSOTrx())
			{
				throw new AdempiereException("Sales invoice line not supported: " + cd);
			}
			return CostingDocumentRef.ofPurchaseInvoiceLineId(cd.getC_InvoiceLine_ID());
		}
		else if (cd.getM_InOutLine_ID() > 0)
		{
			if (!cd.isSOTrx())
			{
				throw new AdempiereException("Material receipt line not supported: " + cd);
			}
			return CostingDocumentRef.ofShipmentLineId(cd.getM_InOutLine_ID());
		}
		else if (cd.getM_MovementLine_ID() > 0)
		{
			return cd.isSOTrx() ? CostingDocumentRef.ofOutboundMovementLineId(cd.getM_MovementLine_ID()) : CostingDocumentRef.ofInboundMovementLineId(cd.getM_MovementLine_ID());
		}
		else if (cd.getM_InventoryLine_ID() > 0)
		{
			return CostingDocumentRef.ofInventoryLineId(cd.getM_InventoryLine_ID());
		}
		else if (cd.getM_ProductionLine_ID() > 0)
		{
			return CostingDocumentRef.ofProductionLineId(cd.getM_ProductionLine_ID());
		}
		else if (cd.getC_ProjectIssue_ID() > 0)
		{
			return CostingDocumentRef.ofProjectIssueId(cd.getC_ProjectIssue_ID());
		}
		else if (cd.getPP_Cost_Collector_ID() > 0)
		{
			return CostingDocumentRef.ofCostCollectorId(cd.getPP_Cost_Collector_ID());
		}
		else
		{
			throw new AdempiereException("Cannot extract " + CostingDocumentRef.class + " from " + cd);
		}
	}

	/**
	 * Process cost detail for cost record
	 */
	private void process(final CostDetailEvent event)
	{
		getCostingMethodHandler(event.getCostingMethod())
				.process(event);
	}

	private static boolean isDelta(final I_M_CostDetail costDetail)
	{
		return !(costDetail.getDeltaAmt().signum() == 0
				&& costDetail.getDeltaQty().signum() == 0);
	}	// isDelta

	private CostDetailEvent createCostDetailEvent(final I_M_CostDetail cd, final CostSegment costSegment, final CostElement costElement)
	{
		final BigDecimal qty;
		final BigDecimal amt;
		if (isDelta(cd))
		{
			qty = cd.getDeltaQty();
			amt = cd.getDeltaAmt();
		}
		else
		{
			qty = cd.getQty();
			amt = cd.getAmt();
		}

		final MAcctSchema as = MAcctSchema.get(Env.getCtx(), costSegment.getAcctSchemaId());
		final int precision = as.getCostingPrecision();
		final BigDecimal price;
		if (qty.signum() != 0)
		{
			price = amt.divide(qty, precision, RoundingMode.HALF_UP);
		}
		else
		{
			price = amt;
		}

		return CostDetailEvent.builder()
				.costSegment(costSegment)
				.costElementId(costElement.getId())
				.costingMethod(costElement.getCostingMethod())
				.documentRef(extractDocumentRef(cd))
				.amt(amt)
				.qty(qty)
				.price(price)
				.precision(precision)
				.build();
	}

	@Override
	public BigDecimal calculateSeedCosts(final CostSegment costSegment, final String costingMethod, final int orderLineId)
	{
		return getCostingMethodHandler(costingMethod)
				.calculateSeedCosts(costSegment, orderLineId);
	}
}
