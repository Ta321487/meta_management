-- 是否参与「新增/编辑」表单录入：1 参与 0 不参与（如业务编码仅展示/系统生成，仍可在列表展示）
ALTER TABLE `metadata_field`
  ADD COLUMN `in_form` tinyint NOT NULL DEFAULT 1 COMMENT '1参与表单录入 0不参与' AFTER `form_component`;
