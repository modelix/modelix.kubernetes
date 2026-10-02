<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:6a47e1fc-fe5e-402e-a149-33253c7b9739(my.solution.model_with_file_per_root_persistence)">
  <persistence version="9" />
  <languages>
    <use id="95dcc83d-7fad-44e8-9994-1ed13a0c7a59" name="my.language" version="0" />
    <devkit ref="fbc25dd2-5da4-483a-8b19-70928e1b62d7(jetbrains.mps.devkit.general-purpose)" />
  </languages>
  <imports>
    <import index="lktc" ref="5a9ccb4c-d683-45a8-bc1d-ecfdfb8366f0/java:gnu.trove(gnu.trove/)" />
  </imports>
  <registry>
    <language id="f3061a53-9226-4cc5-a443-f952ceaf5816" name="jetbrains.mps.baseLanguage">
      <concept id="1070534058343" name="jetbrains.mps.baseLanguage.structure.NullLiteral" flags="nn" index="10Nm6u" />
      <concept id="1068390468198" name="jetbrains.mps.baseLanguage.structure.ClassConcept" flags="ig" index="312cEu" />
      <concept id="1068580123132" name="jetbrains.mps.baseLanguage.structure.BaseMethodDeclaration" flags="ng" index="3clF44">
        <child id="1068580123133" name="returnType" index="3clF45" />
        <child id="1068580123135" name="body" index="3clF47" />
      </concept>
      <concept id="1068580123165" name="jetbrains.mps.baseLanguage.structure.InstanceMethodDeclaration" flags="ig" index="3clFb_" />
      <concept id="1068580123136" name="jetbrains.mps.baseLanguage.structure.StatementList" flags="sn" stub="5293379017992965193" index="3clFbS">
        <child id="1068581517665" name="statement" index="3cqZAp" />
      </concept>
      <concept id="1068581242878" name="jetbrains.mps.baseLanguage.structure.ReturnStatement" flags="nn" index="3cpWs6">
        <child id="1068581517676" name="expression" index="3cqZAk" />
      </concept>
      <concept id="1068581517677" name="jetbrains.mps.baseLanguage.structure.VoidType" flags="in" index="3cqZAl" />
      <concept id="1107461130800" name="jetbrains.mps.baseLanguage.structure.Classifier" flags="ng" index="3pOWGL">
        <child id="5375687026011219971" name="member" index="jymVt" unordered="true" />
      </concept>
      <concept id="1107535904670" name="jetbrains.mps.baseLanguage.structure.ClassifierType" flags="in" index="3uibUv">
        <reference id="1107535924139" name="classifier" index="3uigEE" />
      </concept>
      <concept id="1178549954367" name="jetbrains.mps.baseLanguage.structure.IVisible" flags="ngI" index="1B3ioH">
        <child id="1178549979242" name="visibility" index="1B3o_S" />
      </concept>
      <concept id="1146644602865" name="jetbrains.mps.baseLanguage.structure.PublicVisibility" flags="nn" index="3Tm1VV" />
    </language>
    <language id="ceab5195-25ea-4f22-9b92-103b95ca8c0c" name="jetbrains.mps.lang.core">
      <concept id="1169194658468" name="jetbrains.mps.lang.core.structure.INamedConcept" flags="ngI" index="TrEIO">
        <property id="1169194664001" name="name" index="TrG5h" />
      </concept>
    </language>
  </registry>
  <node concept="312cEu" id="kw_$T6$Guf">
    <property role="TrG5h" value="MyRenamedClassInFilePerRootPersistence" />
    <node concept="3clFb_" id="kw_$T6$GuQ" role="jymVt">
      <property role="TrG5h" value="f" />
      <node concept="3cqZAl" id="kw_$T6$GuS" role="3clF45" />
      <node concept="3Tm1VV" id="kw_$T6$GuT" role="1B3o_S" />
      <node concept="3clFbS" id="kw_$T6$GuU" role="3clF47" />
    </node>
    <node concept="3clFb_" id="kw_$T6$GIN" role="jymVt">
      <property role="TrG5h" value="modifiedRootNodeOnMainBranch" />
      <node concept="3uibUv" id="kw_$T6$HOV" role="3clF45">
        <ref role="3uigEE" to="lktc:~THashSet" resolve="THashSet" />
      </node>
      <node concept="3Tm1VV" id="kw_$T6$GIQ" role="1B3o_S" />
      <node concept="3clFbS" id="kw_$T6$GIR" role="3clF47">
        <node concept="3cpWs6" id="kw_$T6$HPR" role="3cqZAp">
          <node concept="10Nm6u" id="kw_$T6$HQV" role="3cqZAk" />
        </node>
      </node>
    </node>
    <node concept="3Tm1VV" id="kw_$T6$Gug" role="1B3o_S" />
  </node>
  <node concept="312cEu" id="kw_$T6$Igo">
    <property role="TrG5h" value="NewRootNodeInFilePerRootPersistence" />
    <node concept="3Tm1VV" id="kw_$T6$Igp" role="1B3o_S" />
  </node>
</model>

